package com.testquality.jenkins;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.testquality.jenkins.exception.HttpException;
import hudson.util.ListBoxModel;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TestQualityClient {

    void connect(String url, StandardUsernamePasswordCredentials credentials)
            throws IOException, JSONException, HttpException;

    void getList(String type, String keyPrefix, ListBoxModel items, String selectedId, Map<String, String> params)
            throws IOException, JSONException, HttpException;

    TestResult uploadFiles(List<File> files, String projectId, String planId, String milestoneId)
            throws IOException, HttpException;

}
