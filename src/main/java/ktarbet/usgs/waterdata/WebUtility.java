package ktarbet.usgs.waterdata;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP utility for fetching web pages with caching.
 *
 * <p>Provides GET and POST methods with a five-minute in-memory cache
 * to reduce redundant network calls. Supports an optional API key
 * via the {@value #ENV_USGS_WATER_API_KEY} environment variable.
 */
class WebUtility {

    private static final Logger logger = Logger.getLogger(WebUtility.class.getName());

    static final String ENV_USGS_WATER_API_KEY = UsgsApiKeyException.ENV_VAR_NAME;
    private static final long PAGE_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final int PAGE_CACHE_MAX_SIZE = 100;
    private static final ConcurrentHashMap<String, CacheEntry> PAGE_CACHE = new ConcurrentHashMap<>();
    private static boolean apiKeyLogged = false;
    private static final Pattern COLLECTION_NAME_PATTERN =
            Pattern.compile("/collections/(?<collection>[a-z][a-z0-9\\-]*)/items\\b");

    private static class CacheEntry {
        final String body;
        final long fetchedAtMillis;

        CacheEntry(String body, long fetchedAtMillis) {
            this.body = body;
            this.fetchedAtMillis = fetchedAtMillis;
        }
    }

    private WebUtility() {
        // Prevent instantiation
    }

    public static String getPage(String url) throws Exception {
        logger.info("Requesting: " + url);
        HttpRequest request = buildRequest(url).GET().build();
        return fetchPage(url, request);
    }

    public static String postPage(String url, String contentType, String body, String cacheKey) throws Exception {
        logger.info("POST: " + url);
        HttpRequest request = buildRequest(url)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return fetchPage(cacheKey, request);
    }

    static final String GITHUB_URL = "https://github.com/ktarbet/usgs-waterdata-api";

    static HttpRequest.Builder buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        StringBuilder ua = new StringBuilder("usgs-waterdata-api (" + GITHUB_URL + ")");
        String appName = UsgsWaterDataApi.getApplicationName();
        if (appName != null && !appName.isEmpty()) {
            ua.append(" ").append(appName);
        }
        builder.header("User-Agent", ua.toString());

        String apiKey = UsgsWaterDataApi.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            if (!apiKeyLogged) {
                apiKeyLogged = true;
                logger.info("Using " + ENV_USGS_WATER_API_KEY + "=" + maskKey(apiKey));
            }
            builder.header("X-Api-Key", apiKey);
        }
        return builder;
    }

    static String maskKey(String key) {
        int show = Math.min(3, key.length() / 3);
        return key.substring(0, show) + "*".repeat(key.length() - show);
    }

    static String fetchPage(String cacheKey, HttpRequest request) throws Exception {
        long now = System.currentTimeMillis();
        CacheEntry cached = PAGE_CACHE.get(cacheKey);
        if (cached != null) {
            if ((now - cached.fetchedAtMillis) < PAGE_CACHE_TTL_MS) {
                logger.info("Cache hit: " + cacheKey);
                return cached.body;
            }
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        int statusCode = response.statusCode();

        if (statusCode == 403) { // Forbidden - API key is missing or not authorized for this resource.
            String key = UsgsWaterDataApi.getApiKey();
            throw new UsgsApiKeyException(UsgsApiKeyException.Reason.FORBIDDEN,
                    "unknown", key != null && !key.isEmpty());
        }
        if (statusCode == 429) { // Too Many Requests - Rate limit exceeded.
            String rateLimit = response.headers().firstValue("X-RateLimit-Limit").orElse("unknown");
            String key = UsgsWaterDataApi.getApiKey();
            throw new UsgsApiKeyException(UsgsApiKeyException.Reason.RATE_LIMIT_EXCEEDED,
                    rateLimit, key != null && !key.isEmpty());
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new RuntimeException("HTTP " + statusCode
                    + " for " + request.uri()
                    + "\nResponse: " + responseBody.substring(0, Math.min(responseBody.length(), 500)));
        }

        String rateLimit = response.headers().firstValue("X-RateLimit-Limit").orElse("unknown");
        response.headers().firstValue("X-RateLimit-Remaining")
                .ifPresent(v -> {
                    logger.info("Rate limit: " + v + " of " + rateLimit + " remaining");
                    if ("0".equals(v.trim())) {
                        logger.warning("Rate limit exhausted (0 of " + rateLimit
                                + " remaining). Next request will likely be throttled.");
                    }
                });

        
        if (Boolean.getBoolean("usgs.debug")) {
            String prefix = filenamePrefixFromUrl(response.uri().toString());
            saveForDebugging(response, responseBody, prefix);
        }

        if (PAGE_CACHE.size() >= PAGE_CACHE_MAX_SIZE) {
            PAGE_CACHE.clear();
        }
        PAGE_CACHE.put(cacheKey, new CacheEntry(responseBody, now));
        return responseBody;
    }

    static String filenamePrefixFromUrl(String url) {
        Matcher m = COLLECTION_NAME_PATTERN.matcher(url);
        if (m.find()) {
            return m.group("collection");
        }
        return "";
    }

    private static void saveForDebugging(HttpResponse<String> response, String body, String filenamePrefix) {
        try {
            Optional<String> disposition = response.headers().firstValue("Content-Disposition");
            String filename = null;
            if (disposition.isPresent()) {
                for (String part : disposition.get().split(";")) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("filename=")) {
                        filename = filenamePrefix + "_" + trimmed.substring("filename=".length()).replace("\"", "").trim();
                    }
                }
            }
            if (filename == null || filename.isEmpty()) {
                // Use the prefix (e.g. "continuous") as the base name
                if (!filenamePrefix.isEmpty()) {
                    filename = filenamePrefix;
                } else {
                    filename = "response";
                }
                // Determine extension from f= query parameter
                String query = response.uri().getQuery();
                String ext = ".txt";
                if (query != null && query.contains("f=csv")) {
                    ext = ".csv";
                }
                filename += ext;
            }
            Path dir = Paths.get(System.getProperty("user.home"), "usgs.waterdata");
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            if (Files.exists(file)) {
                String name = filename;
                String ext = "";
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) {
                    name = filename.substring(0, dot);
                    ext = filename.substring(dot);
                }
                int n = 1;
                while (Files.exists(file)) {
                    file = dir.resolve(name + n + ext);
                    n++;
                }
            }
            Files.writeString(file, body);
            logger.info("Saved response to: " + file);
        } catch (Exception e) {
            logger.warning("Failed to save debug file: " + e.getMessage());
        }
    }
}
