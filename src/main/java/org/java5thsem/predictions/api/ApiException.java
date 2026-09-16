package org.java5thsem.predictions.api;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public ApiException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException userNotFound(Long userId) {
        return new ApiException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND,
                "User %d was not found".formatted(userId));
    }

    public static ApiException matchNotFound(Long matchId) {
        return new ApiException(ErrorCode.MATCH_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Match %d was not found".formatted(matchId));
    }

    public static ApiException predictionNotFound(Long userId, Long matchId) {
        return new ApiException(ErrorCode.PREDICTION_NOT_FOUND, HttpStatus.NOT_FOUND,
                "No prediction exists for user %d and match %d".formatted(userId, matchId));
    }

    public static ApiException duplicatePrediction(Long userId, Long matchId) {
        return new ApiException(ErrorCode.DUPLICATE_PREDICTION, HttpStatus.CONFLICT,
                "A prediction already exists for user %d and match %d".formatted(userId, matchId));
    }

    public static ApiException matchNotAvailable(Long matchId) {
        return new ApiException(ErrorCode.MATCH_NOT_AVAILABLE, HttpStatus.UNPROCESSABLE_ENTITY,
                "Match %d is not available for predictions".formatted(matchId));
    }

    public static ApiException matchAlreadyStarted(Long matchId) {
        return new ApiException(ErrorCode.MATCH_ALREADY_STARTED, HttpStatus.UNPROCESSABLE_ENTITY,
                "Predictions cannot be created or changed after match %d has started".formatted(matchId));
    }

    public static ApiException deadlinePassed(Long matchId) {
        return new ApiException(ErrorCode.PREDICTION_DEADLINE_PASSED, HttpStatus.UNPROCESSABLE_ENTITY,
                "The prediction deadline for match %d has passed".formatted(matchId));
    }

    public static ApiException providerNotConfigured() {
        return new ApiException(ErrorCode.PROVIDER_NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE,
                "API-Football is not configured. Set the API_FOOTBALL_KEY environment variable");
    }

    public static ApiException providerUnavailable(String detail) {
        return new ApiException(ErrorCode.PROVIDER_UNAVAILABLE, HttpStatus.BAD_GATEWAY,
                detail);
    }

    public static ApiException providerRateLimited() {
        return new ApiException(ErrorCode.PROVIDER_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS,
                "API-Football rate limit was exceeded. Try again later");
    }

    public static ApiException matchweekNotFound(int matchweek, int season) {
        return new ApiException(ErrorCode.MATCHWEEK_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Matchweek %d was not found for Premier League season %d".formatted(matchweek, season));
    }

    public static ApiException invalidDateRange(String message) {
        return new ApiException(ErrorCode.INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException pushTokenNotFound(Long tokenId) {
        return new ApiException(ErrorCode.PUSH_TOKEN_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Push token %d was not found".formatted(tokenId));
    }

    public static ApiException authProviderUnsupported(String provider) {
        return new ApiException(ErrorCode.AUTH_PROVIDER_UNSUPPORTED, HttpStatus.BAD_REQUEST,
                "Social login provider '%s' is not supported".formatted(provider));
    }

    public static ApiException authProviderNotConfigured(String provider) {
        return new ApiException(ErrorCode.AUTH_PROVIDER_NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE,
                "%s social login is not configured".formatted(provider));
    }

    public static ApiException invalidSocialToken() {
        return new ApiException(ErrorCode.INVALID_SOCIAL_TOKEN, HttpStatus.UNAUTHORIZED,
                "The social login token is invalid or has expired");
    }

    public static ApiException socialAccountConflict() {
        return new ApiException(ErrorCode.SOCIAL_ACCOUNT_CONFLICT, HttpStatus.CONFLICT,
                "This social account cannot be linked because the email is already used with a different account");
    }

    public static ApiException authSessionInvalid() {
        return new ApiException(ErrorCode.AUTH_SESSION_INVALID, HttpStatus.UNAUTHORIZED,
                "A valid Bearer access token is required");
    }
}
