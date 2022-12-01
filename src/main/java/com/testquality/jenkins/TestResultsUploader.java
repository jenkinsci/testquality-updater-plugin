package com.testquality.jenkins;

import com.testquality.jenkins.exception.CredentialsException;
import com.testquality.jenkins.exception.HttpException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestResultsUploader {

    private final String plan;
    private final String milestone;
    private final String testResults;
    private final String project;

    public TestResultsUploader(String plan, String milestone, String testResults, String project) {
        this.plan = plan;
        this.milestone = milestone;
        this.testResults = testResults;
        this.project = project;
    }

    public boolean upload(TaskListener listener, Run run, FilePath workspace) {
        try {
            final String expandTestResults = run.getEnvironment(listener).expand(this.testResults);
            final long buildTime = run.getTimestamp().getTimeInMillis();
            final long timeOnMaster = System.currentTimeMillis();

            if (run.getResult() == Result.FAILURE) {
                // most likely a build failed before it gets to the test phase.
                // don't continue.
                return true;
            }
            listener.getLogger().println("Starting test upload to TestQuality for" + this.project);

            TestResult result = workspace.act(
                    new ParseResultCallable(
                            expandTestResults,
                            buildTime,
                            timeOnMaster,
                            this.project,
                            this.plan,
                            this.milestone
                    )
            );

            long time = System.currentTimeMillis() - timeOnMaster;
            if (result.total > 0) {
                listener.getLogger().printf("Of %d tests, %d passed, %d failed, %d skipped, tests ran in %s seconds%n",
                        result.total, result.passed, result.failed, result.blocked, result.time);
            }
            if (!StringUtils.isBlank(result.run_url)) {
                //listener.getLogger().println(String.format("View Test Run Result at %s",
                //        result.run_url));
                listener.hyperlink(result.run_url, "View Test Run Result (" + result.run_url + ")\n");
            }
            listener.getLogger().printf("TestQuality Upload finished in %sms%n", time);
        } catch (InterruptedException e) {
            listener.getLogger().println("Interupted, " + e.getMessage());
            return false;
        } catch (JSONException | IOException | HttpException | CredentialsException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        }

        return true;
    }

    private static final class ParseResultCallable extends MasterToSlaveFileCallable<TestResult> {
        private final String testResults;
        private final long buildTime;
        private final long nowMaster;
        private final String project;
        private final String plan;
        private final String milestone;

        private ParseResultCallable(String testResults,
                                    long buildTime,
                                    long nowMaster,
                                    String project,
                                    String plan,
                                    String milestone) {
            this.testResults = testResults;
            this.buildTime = buildTime;
            this.nowMaster = nowMaster;
            this.plan = plan;
            this.project = project;
            this.milestone = milestone;
        }

        @Override
        public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
            final long nowSlave = System.currentTimeMillis();
            List<File> listFiles = new ArrayList<>();

            FileSet fs = Util.createFileSet(ws, testResults);
            DirectoryScanner ds = fs.getDirectoryScanner();
            TestResult result = new TestResult();

            String[] files = ds.getIncludedFiles();
            if (files.length > 0) {

                File baseDir = ds.getBasedir();
                //result = new TestResult(buildTime + (nowSlave - nowMaster), ds, keepLongStdio);
                for (String value : files) {
                    File reportFile = new File(baseDir, value);
                    // only count files that were actually updated during this build
                    if (this.buildTime + (nowSlave - this.nowMaster) - 3000/*error margin*/ <= reportFile.lastModified()) {
                        listFiles.add(reportFile);
                        //parsePossiblyEmpty(reportFile);
                        //parsed = true;
                    }
                }

                TestQualityClient testQuality = TestQualityClientFactory.create();
                return testQuality.uploadFiles(listFiles, this.project, this.plan, this.milestone);
            }
            return result;
        }
    }
}
