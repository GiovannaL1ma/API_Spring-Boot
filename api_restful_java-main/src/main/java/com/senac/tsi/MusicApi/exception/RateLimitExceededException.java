package com.senac.tsi.MusicApi.exception;

// o cliente passou do limite de requisicoes por minuto; o ApiExceptionAdvice devolve 429 com o header Retry-After
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(int limit, long retryAfterSeconds) {
        super("Too many requests: the limit is " + limit + " requests per minute. Try again in " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
