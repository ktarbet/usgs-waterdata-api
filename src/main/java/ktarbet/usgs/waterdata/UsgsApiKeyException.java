package ktarbet.usgs.waterdata;

/**
 * Thrown when the USGS Water Data API returns an error related to API key
 * authorization or rate limiting (HTTP 403 or 429).
 *
 * <p>Client applications can catch this single exception to present targeted
 * guidance — for example, prompting the user to register for an API key
 * or to wait before retrying.
 */
public class UsgsApiKeyException extends RuntimeException {

    public enum Reason {
        /** 403 Forbidden — API key is missing or not authorized. */
        FORBIDDEN,
        /** 429 Too Many Requests — rate limit exceeded. */
        RATE_LIMIT_EXCEEDED
    }

    public static final String SIGNUP_URL = "https://api.waterdata.usgs.gov/signup/";
    public static final String ENV_VAR_NAME = "USGS_WATER_API_KEY";

    private final Reason reason;
    private final String rateLimit;
    private final boolean apiKeyPresent;

    public UsgsApiKeyException(Reason reason, String rateLimit, boolean apiKeyPresent) {
        super(buildMessage(reason, rateLimit, apiKeyPresent));
        this.reason = reason;
        this.rateLimit = rateLimit;
        this.apiKeyPresent = apiKeyPresent;
    }

    /** Why the request was rejected. */
    public Reason getReason() {
        return reason;
    }

    /** The value of the {@code X-RateLimit-Limit} header, or {@code "unknown"}. */
    public String getRateLimit() {
        return rateLimit;
    }

    /** Whether the request included an API key. */
    public boolean isApiKeyPresent() {
        return apiKeyPresent;
    }

    private static String buildMessage(Reason reason, String rateLimit, boolean apiKeyPresent) {
        String msg;
        switch (reason) {
            case FORBIDDEN:
                msg = "HTTP 403 Forbidden. ";
                if (apiKeyPresent) {
                    msg += "Your API key is not authorized for this resource.";
                } else {
                    msg += "No API key detected. Register for an API key at " + SIGNUP_URL
                            + " and set the " + ENV_VAR_NAME
                            + " environment variable.";
                }
                break;
            case RATE_LIMIT_EXCEEDED:
                msg = "USGS API rate limit exceeded (0 of " + rateLimit + " requests remaining). ";
                if (apiKeyPresent) {
                    msg += "Consider using a different API key or waiting before making more requests.";
                } else {
                    msg += "No API key detected. Register for an API key at " + SIGNUP_URL
                            + " and set the " + ENV_VAR_NAME
                            + " environment variable to increase your rate limit.";
                }
                break;
            default:
                msg = "USGS API key error.";
        }
        return msg;
    }
}
