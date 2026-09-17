package de.mrjulsen.crn.web.api;

public final class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
