package de.mrjulsen.crn.exceptions;

public class RuntimeSideException extends RuntimeException {
    public RuntimeSideException(boolean expectClient) {
        super("This method can only be called on the " + (expectClient ? "client" : "server") + " side!");
    }
}
