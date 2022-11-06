package com.testquality.jenkins;

import com.testquality.jenkins.exception.HttpException;
import hudson.util.ListBoxModel;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface TestQualityClient {

    void connect(String url, String username, String password)
            throws IOException, JSONException, HttpException;

    void getList(String type, String keyPrefix, ListBoxModel items, String selectedId, String projectId)
            throws IOException, JSONException, HttpException;

    TestResult uploadFiles(List<File> files, String planId, String milestoneId)
            throws IOException, HttpException;

}
