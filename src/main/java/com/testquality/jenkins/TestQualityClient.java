package com.testquality.jenkins;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.testquality.jenkins.exception.HttpException;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TestQualityClient {

    void connect(String url, StandardUsernamePasswordCredentials credentials)
            throws IOException, JSONException, HttpException;

    List<TestQualityBaseResponse> getList(String type, Map<String, String> params)
            throws IOException, JSONException, HttpException;

    TestResult uploadFiles(List<File> files, String projectId, String planId, String milestoneId)
            throws IOException, HttpException;

    default List<TestQualityBaseResponse> projects() throws IOException {
        return getList("project", null);
    }

    default List<TestQualityBaseResponse> milestones(Map<String, String> params) throws IOException {
        return getList("milestone", params);
    }

    default List<TestQualityBaseResponse> cycles(Map<String, String> params) throws IOException {
        return getList("plan", params);
    }

}
