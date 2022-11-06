package com.testquality.jenkins;

import com.testquality.jenkins.exception.ClientException;

import java.io.IOException;

public class TestQualityClientFactory {

    private TestQualityClientFactory() {
        //
    }

    public static HttpTestQuality create(String url, String username ,String password) {
        HttpTestQuality testQuality = new HttpTestQuality();
        try {
            testQuality.connect(url, username, password);
        } catch (IOException | HttpException e) {
            throw new ClientException(e);
        }
        return testQuality;
    }

    public static HttpTestQuality create() {
        TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
        return create(configuration.getUrl(), configuration.getUsername(), configuration.getPassword());
    }

}
