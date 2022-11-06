package com.testquality.jenkins.exception;

import java.io.IOException;

public class ClientException extends RuntimeException {
    public ClientException(IOException message) {
        super(message);
    }
}
