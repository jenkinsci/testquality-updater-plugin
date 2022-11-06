package com.testquality.jenkins.exception;

public class ClientException extends RuntimeException {
    public ClientException(Throwable message) {
        super(message);
    }
}
