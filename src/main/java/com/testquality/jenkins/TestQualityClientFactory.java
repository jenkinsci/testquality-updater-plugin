package com.testquality.jenkins;

import com.testquality.jenkins.exception.ClientException;

import java.io.IOException;

public class TestQualityClientFactory {

    private TestQualityClientFactory() {
        //
    }

    public static HttpTestQuality create() {
        TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
        HttpTestQuality testQuality = new HttpTestQuality();
        try {
            testQuality.connect(configuration.getUrl(), configuration.getUsername(), configuration.getPassword());
        } catch (IOException e) {
            throw new ClientException(e);
        }
        return testQuality;
    }

}
