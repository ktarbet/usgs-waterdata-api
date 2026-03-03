package hec.usgs.waterdata;

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

/**
 * HTTP utility for fetching web pages with caching.
 *
 * <p>Provides GET and POST methods with a five-minute in-memory cache
 * to reduce redundant network calls. Supports an optional API key
 * via the {@value #ENV_USGS_WATER_API_KEY} environment variable.
 */
public class WebUtility {

    private static final Logger logger = Logger.getLogger(WebUtility.class.getName());

    static final String ENV_USGS_WATER_API_KEY = "USGS_WATER_API_KEY";
    private static final long PAGE_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final int PAGE_CACHE_MAX_SIZE = 100;
    private static final ConcurrentHashMap<String, CacheEntry> PAGE_CACHE = new ConcurrentHashMap<>();
    private static boolean apiKeyLogged = false;

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

    static HttpRequest.Builder buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));
        String apiKey = System.getenv(ENV_USGS_WATER_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            if (!apiKeyLogged) {
                apiKeyLogged = true;
                logger.info("Using " + ENV_USGS_WATER_API_KEY + "=" + apiKey.substring(0, 3) + "...");
            }
            builder.header("X-Api-Key", apiKey);
        }
        return builder;
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
                        throw new RuntimeException(
                                "USGS API rate limit exceeded (0 of " + rateLimit + " requests remaining). "
                                + "Register for an API key at https://api.waterdata.usgs.gov/signup/ "
                                + "and set the " + ENV_USGS_WATER_API_KEY + " environment variable.");
                    }
                });
        if (Boolean.getBoolean("usgs.debug")) {
            saveForDebugging(response, responseBody);
        }

        if (PAGE_CACHE.size() >= PAGE_CACHE_MAX_SIZE) {
            PAGE_CACHE.clear();
        }
        PAGE_CACHE.put(cacheKey, new CacheEntry(responseBody, now));
        return responseBody;
    }

    private static void saveForDebugging(HttpResponse<String> response, String body) {
        try {
            Optional<String> disposition = response.headers().firstValue("Content-Disposition");
            String filename = null;
            if (disposition.isPresent()) {
                for (String part : disposition.get().split(";")) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("filename=")) {
                        filename = trimmed.substring("filename=".length()).replace("\"", "").trim();
                    }
                }
            }
            if (filename == null || filename.isEmpty()) {
                logger.info("No filename found in Content-Disposition header");
                return;
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
