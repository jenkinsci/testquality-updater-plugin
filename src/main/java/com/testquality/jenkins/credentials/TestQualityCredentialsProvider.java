package com.testquality.jenkins.credentials;

@FunctionalInterface
public interface TestQualityCredentialsProvider {
    TestQualityCredentials resolve();
}
