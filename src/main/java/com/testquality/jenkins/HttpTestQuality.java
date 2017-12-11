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

import hudson.util.ListBoxModel;
import java.io.File;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.json.JSONObject;


import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 * @author jamespitts
 */
public class HttpTestQuality {

    /**
     *
     */
    private final OkHttpClient client; // = new OkHttpClient();
    private String tqUrl;
    private String authorization;
    
    public HttpTestQuality() {
        this.client = new OkHttpClient.Builder()
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        //this.client.setConnectTimeout(30, TimeUnit.SECONDS);
        
    }
    
    public boolean isConnected() {
        if (authorization != null) {
            return StringUtils.isBlank(authorization);
        } else {
            return false;
        }
    }
    
    public void connect(String url, String username, String password) throws IOException, JSONException, HttpException {
        this.tqUrl = url;
        FormBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("client_id", "2")
                .add("client_secret", "93MBS86X7JrK4Mrr1mk4PKfo6b1zRVx9Mrmx0nTa")
                .add("username", username)
                .add("password", password)
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
    
    public void getList(String type, String keyPrefix, ListBoxModel items, String selectedId, String projectId) 
            throws IOException, JSONException, HttpException {
        
        String url = this.tqUrl + "/api/" + type;
        if (!StringUtils.isBlank(projectId)) {
            url = url + "?project_id=" + projectId;
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
        for (int i = 0; i < arr.length(); i++)
        {
            String name = arr.getJSONObject(i).getString("name");
            int key = arr.getJSONObject(i).getInt("key");
            int id = arr.getJSONObject(i).getInt("id");
            String idStr = Integer.toString(id);
            
            items.add(new ListBoxModel.Option(String.format("%s%s %s", keyPrefix, key, name), idStr, idStr.equals(selectedId)));
        }
    }
    
    public TestResult uploadFiles(List<File> files, String planId, String milestoneId) throws IOException, HttpException {
        if (files.isEmpty()) {
            throw new HttpException("No files selected for upload");
        }
                
        MediaType mediaType = MediaType.parse("text/xml");
        String url = this.tqUrl + "/api/plan/" + planId + "/junit_xml";
        
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder();
        requestBodyBuilder.setType(MultipartBody.FORM);
        if (!StringUtils.isBlank(milestoneId) && !milestoneId.equals("-1")) {
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
        if (obj.has("start_time")) {
            result.start_time = obj.getString("start_time");
        }
        if (obj.has("end_time")) {
            result.end_time = obj.getString("end_time");
        }
        if (obj.has("run_url")) {
            result.run_url = obj.getString("run_url");
        }
        return result;
    }
}
