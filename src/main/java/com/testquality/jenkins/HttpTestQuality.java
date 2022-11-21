/*
 * The MIT License
 *
 * Copyright 2017 BitModern.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.testquality.jenkins;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.testquality.jenkins.exception.HttpException;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 *
 * @author jamespitts
 */
public class HttpTestQuality implements TestQualityClient {
    private final OkHttpClient client;
    private String tqUrl;
    private String authorization;

    public HttpTestQuality() {
        this.client = new OkHttpClient.Builder()
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConnected() {
        if (authorization != null) {
            return isBlank(authorization);
        } else {
            return false;
        }
    }

    public void connect(String url, StandardUsernamePasswordCredentials credentials) throws IOException, JSONException, HttpException {
        this.tqUrl = url;
        FormBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("client_id", "2")
                .add("client_secret", "93MBS86X7JrK4Mrr1mk4PKfo6b1zRVx9Mrmx0nTa")
                .add("username", credentials.getUsername())
                .add("password", credentials.getPassword().getPlainText())
                .build();

        Request.Builder builder = new Request.Builder()
                .url(url + "/api/oauth/access_token")
                .post(formBody);

        Request request = builder.build();

        long start = System.nanoTime();
        Response response = this.client.newCall(request).execute();
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        if (!response.isSuccessful()) {
            throw new HttpException(response, time, response.body().string());
        }

        String json = response.body().string();
        JSONObject obj;
        obj = new JSONObject(json);
        if (obj.has("verification_ended_at")) {
            throw new HttpException("User account needs to be verified, please check inbox.");
        }
        this.authorization = "Bearer " + obj.getString("access_token");
    }

    @Override
    public List<TestQualityBaseResponse> getList(String type,  Map<String, String> params)
            throws IOException, JSONException, HttpException {

        String url = this.tqUrl + "/api/" + type;

        if (params != null) {
            String query = params.entrySet()
                    .stream()
                    .map((e)-> e.getKey()+"=" + e.getValue())
                    .collect(Collectors.joining("&"));
            if (isNotBlank(query)) {
                url += "?" + query;
            }
        }

        Request.Builder builder = new Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .addHeader("Authorization", this.authorization);

        Request request = builder.build();

        long start = System.nanoTime();
        Response response = this.client.newCall(request).execute();
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        if (!response.isSuccessful()) {
            throw new HttpException(response, time, response.body().string());
        }

        JSONObject obj = new JSONObject(response.body().string());
        JSONArray arr = obj.getJSONArray("data");

        List<TestQualityBaseResponse> listObjects = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++)
        {
            TestQualityBaseResponse r = new TestQualityBaseResponse();
            JSONObject jObj = arr.getJSONObject(i);
            r.setKey(jObj.getInt("key"));
            r.setId(jObj.getInt("id"));
            r.setName(jObj.getString("name"));
            listObjects.add(r);
        }

        return listObjects;
    }

    @Override
    public TestResult uploadFiles(List<File> files, String projectId, String planId, String milestoneId) throws IOException, HttpException {
        if (files.isEmpty()) {
            throw new HttpException("No files selected for upload");
        }
        String calculatedPlanId = isDefined(planId) ? planId : getRootPlan(projectId);

        MediaType mediaType = MediaType.parse("text/xml");
        String url = this.tqUrl + "/api/plan/" + calculatedPlanId + "/junit_xml";

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder();
        requestBodyBuilder.setType(MultipartBody.FORM);
        if (isDefined(milestoneId)) {
            requestBodyBuilder.addFormDataPart("milestone_id", milestoneId);
        }
        for (File file : files) {
            RequestBody fileBody = RequestBody.create(mediaType, file);
            requestBodyBuilder.addFormDataPart("files[]", file.getName(), fileBody);
        }
        MultipartBody requestBody = requestBodyBuilder.build();
        final Request request = new Request.Builder()
               .header("Accept", "application/json")
               .addHeader("Authorization", this.authorization)
               .url(url)
               .post(requestBody)
               .build();

        long start = System.nanoTime();
        Response response = this.client.newCall(request).execute();
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        if (!response.isSuccessful()) {
            throw new HttpException(response, time, response.body().string());
        }

        TestResult result = new TestResult();
        JSONObject obj = new JSONObject(response.body().string());
        if (obj.has("total")) {
            result.total = obj.getInt("total");
        }
        if (obj.has("passed")) {
            result.passed = obj.getInt("passed");
        }
        if (obj.has("failed")) {
            result.failed = obj.getInt("failed");
        }
        if (obj.has("blocked")) {
            result.blocked = obj.getInt("blocked");
        }
        if (obj.has("time")) {
            result.time = obj.getString("time");
        }
        if (obj.has("run_url")) {
            result.run_url = obj.getString("run_url");
        }
        return result;
    }

    private String getRootPlan(String projectId) throws IOException, JSONException, HttpException {
        Map<String, String> params = new HashMap<>();
        params.put("project_id", projectId);
        params.put("is_root", "true");

        List<TestQualityBaseResponse> response = getList("plan", params);
        return String.valueOf(response.get(0).getId());
    }
    private boolean isDefined(String v){
        return isNotBlank(v) && !v.equals("-1");
    }
}
