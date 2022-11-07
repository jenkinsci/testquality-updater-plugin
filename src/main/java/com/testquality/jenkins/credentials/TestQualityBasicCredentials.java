package com.testquality.jenkins.credentials;

public class TestQualityBasicCredentials implements TestQualityCredentials {

    private final String username;
    private final String password;

    public TestQualityBasicCredentials(String username, String password) {
        validateInputString(username, "Username cannot be blank.");
        validateInputString(password, "Password cannot be blank.");
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
