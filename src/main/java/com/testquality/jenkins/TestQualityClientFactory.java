package com.testquality.jenkins;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.HttpException;

import java.io.IOException;

public class TestQualityClientFactory {

    private TestQualityClientFactory() {
        //
    }

    public static TestQualityClient create(String url, StandardUsernamePasswordCredentials standardCredentials) {
        HttpTestQuality testQuality = new HttpTestQuality();
        try {
            testQuality.connect(url, standardCredentials);
        } catch (IOException | HttpException e) {
            throw new ClientException(e);
        }
        return testQuality;
    }

    public static TestQualityClient create() {
        TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
        return create(configuration.getUrl(), configuration.getCredentials());
    }

}
