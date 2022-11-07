package com.testquality.jenkins;

import com.testquality.jenkins.credentials.TestQualityBasicCredentials;
import com.testquality.jenkins.credentials.TestQualityCredentialsProvider;
import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.HttpException;

import java.io.IOException;

public class TestQualityClientFactory {

    private TestQualityClientFactory() {
        //
    }

    public static TestQualityClient create(String url, TestQualityCredentialsProvider credentialsProvider) {
        HttpTestQuality testQuality = new HttpTestQuality();
        try {
            testQuality.connect(url, credentialsProvider.resolve());
        } catch (IOException | HttpException e) {
            throw new ClientException(e);
        }
        return testQuality;
    }

    public static TestQualityClient create() {
        TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
        TestQualityCredentialsProvider credentialsProvider = () -> new TestQualityBasicCredentials(
                configuration.getUsername(), configuration.getPassword()
        );
        return create(configuration.getUrl(), credentialsProvider);
    }

}
