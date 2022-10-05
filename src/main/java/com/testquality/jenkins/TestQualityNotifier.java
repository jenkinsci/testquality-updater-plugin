/*
 * The MIT License
 *
 * Copyright 2017 BitModern
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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.json.JSONException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author jamespitts
 */
public class TestQualityNotifier extends Notifier {
    public static final String PLUGIN_SHORTNAME = "testquality-updater";

    private String project;
    private String plan;
    private String milestone;
    private String testResults;

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @DataBoundConstructor
    public TestQualityNotifier(String project, String plan, String milestone, String testResults) {
        this.project = project;
        this.plan = plan;
        this.milestone = milestone;
        this.testResults = testResults;
    }

    public String getProject() {
        return project;
    }

    public String getPlan() {
        return plan;
    }

    public String getMilestone() {
        return milestone;
    }

    public String getTestResults() {
        return testResults;
    }

    @Override
    public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, AbortException {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new AbortException("no workspace for " + build);
        }
        try {
            final String expandTestResults = build.getEnvironment(listener).expand(this.testResults);
            final long buildTime = build.getTimestamp().getTimeInMillis();
            final long timeOnMaster = System.currentTimeMillis();

            if (build.getResult() == Result.FAILURE) {
                // most likely a build failed before it gets to the test phase.
                // don't continue.
                return true;
            }
            listener.getLogger().println("Starting test upload to TestQuality for" + this.project);

            StandardUsernamePasswordCredentials standardCredentials = this.getDescriptor().getCredentials();


            TestResult result = workspace.act(new ParseResultCallable(expandTestResults,
                    buildTime,
                    timeOnMaster,
                    this.getDescriptor().getUrl(),
                    standardCredentials.getUsername(),
                    standardCredentials.getPassword().getPlainText(),
                    this.plan,
                    this.milestone));

            long time = System.currentTimeMillis() - timeOnMaster;
            if (result.total > 0) {
                listener.getLogger().println(String.format("Of %d tests, %d passed, %d failed, %d skipped, tests ran in %s seconds",
                        result.total, result.passed, result.failed, result.blocked, result.time));
            }
            if (!StringUtils.isBlank(result.run_url)) {
                //listener.getLogger().println(String.format("View Test Run Result at %s",
                //        result.run_url));
                listener.hyperlink(result.run_url, "View Test Run Result (" + result.run_url + ")\n");
            }
            listener.getLogger().println(String.format("TestQuality Upload finished in %sms", time));
        } catch (InterruptedException e) {
            listener.getLogger().println("Interupted, " + e.getMessage());
            return false;
        } catch (JSONException | IOException | HttpException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        }

        return true;
    }


    private static final class ParseResultCallable extends MasterToSlaveFileCallable<TestResult> {
        private final String testResults;
        private final long buildTime;
        private final long nowMaster;
        private final String url;
        private final String username;
        private final String password;
        private final String plan;
        private final String milestone;

        private ParseResultCallable(String testResults,
                long buildTime,
                long nowMaster,
                String url,
                String username,
                String password,
                String plan,
                String milestone) {
            this.testResults = testResults;
            this.buildTime = buildTime;
            this.nowMaster = nowMaster;
            this.url = url;
            this.username = username;
            this.password = password;
            this.plan = plan;
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
                HttpTestQuality testQuality = new HttpTestQuality();
                testQuality.connect(this.url, this.username, this.password);
                return testQuality.uploadFiles(listFiles, this.plan, this.milestone);
            }
            return result;
        }
    }




    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public static final String DEFAULT_URL = "https://api.testquality.com";
        public static final String NO_CONNECTION = "Please fill in connection details in Manage Jenkins -> Configure System";
        public static final String DISPLAY_NAME = "TestQuality Updater";
        private static final Logger LOGGER = Logger.getLogger("TestQualityPlugin.log");
        private String url;
        private String credentialsId;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            this.url = DescriptorImpl.DEFAULT_URL;
            load();
        }

        @Override
        public String getDisplayName() {
            return DescriptorImpl.DISPLAY_NAME;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/" + PLUGIN_SHORTNAME + "/help/help.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public FormValidation doTestConnection(
            @QueryParameter("url") String url,
            @QueryParameter("credentialsId") String credentialsId) {
            try {
                StandardUsernamePasswordCredentials standardCredentials = getCredentials();


                HttpTestQuality testQuality = new HttpTestQuality();
                testQuality.connect(url, standardCredentials.getUsername(), standardCredentials.getPassword().getPlainText());
                return FormValidation.ok("Successful Connection");
            } catch (JSONException | IOException | HttpException e) {
                return FormValidation.error("Connection error : " + e.getMessage());
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            this.url = formData.getString("url");
            this.credentialsId = formData.getString("credentialsId");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }



        public String getUrl() {
            return url;
        }

        public String getCredentialsId() {
            return credentialsId;
        }

        public StandardUsernamePasswordCredentials getCredentials() {
            return CredentialsMatchers.firstOrNull(
                    CredentialsProvider
                    .lookupCredentials(StandardUsernamePasswordCredentials.class,
                            Jenkins.getInstance(), null,
                            fromUri(this.url).build()),
                    CredentialsMatchers.withId(this.credentialsId));
        }


        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String url,
                                                    @QueryParameter String credentialsId) {
            Job owner = null;

            List<DomainRequirement> apiEndpoint = URIRequirementBuilder.fromUri(url).build();

            return new StandardUsernameListBoxModel()
                    .withEmptySelection()
                    .withAll(CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, owner, null, apiEndpoint));
        }

        public FormValidation doCheckProject(@QueryParameter("project") String project) throws IOException {
            StandardUsernamePasswordCredentials standardCredentials = getCredentials();

            if (StringUtils.isBlank(this.url) || standardCredentials == null) {
                    return FormValidation.error(NO_CONNECTION);
            }

            HttpTestQuality testQuality = new HttpTestQuality();
            try {
                testQuality.connect(this.url, standardCredentials.getUsername(), standardCredentials.getPassword().getPlainText());
            } catch (JSONException | IOException | HttpException e) {
                return FormValidation.error("Connection error : " + e.getMessage());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillProjectItems(@QueryParameter("project") String savedProject) throws FormValidation {
            ListBoxModel items = new ListBoxModel();

            StandardUsernamePasswordCredentials standardCredentials = getCredentials();

            if (StringUtils.isBlank(this.url) || standardCredentials == null) {
                    return items;
            }

            if (StringUtils.isBlank(savedProject)) {
                items.add(new ListBoxModel.Option("", "", true));
            }

            HttpTestQuality testQuality = new HttpTestQuality();
            try {
                testQuality.connect(this.url, standardCredentials.getUsername(), standardCredentials.getPassword().getPlainText());
                testQuality.getList("project", "PJ", items, savedProject, "");
            } catch (JSONException | IOException | HttpException e) {
                LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
                //Don't think this does anything throw FormValidation.error("Connection error : " + e.getMessage(), e);
            }
            return items;
	}

        public ListBoxModel doFillPlanItems(
			@QueryParameter String project,
                        @QueryParameter("plan") String savedPlan) {
            StandardUsernamePasswordCredentials standardCredentials = getCredentials();

            ListBoxModel items = new ListBoxModel();

            if (StringUtils.isBlank(project)
                    || project.trim().equals("-1")) {
                return items;
            }
            HttpTestQuality testQuality = new HttpTestQuality();
            try {
                testQuality.connect(this.url, standardCredentials.getUsername(), standardCredentials.getPassword().getPlainText());
                testQuality.getList("plan", "P", items, savedPlan, project);
            } catch (JSONException | IOException | HttpException e) {
                LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
                //Don't think this does anything throw FormValidation.error("Connection error : " + e.getMessage(), e);
            }
            return items;
        }

        public ListBoxModel doFillMilestoneItems(
			@QueryParameter String project,
                        @QueryParameter("milestone") String savedMilestone) {
            StandardUsernamePasswordCredentials standardCredentials = getCredentials();

            ListBoxModel items = new ListBoxModel();

            if (StringUtils.isBlank(project)
                    || project.trim().equals(NO_CONNECTION)
                    || project.trim().equals("-1")) {
                return items;
            }

            items.add("Optionally Pick Milestone", "-1");

            HttpTestQuality testQuality = new HttpTestQuality();
            try {
                testQuality.connect(this.url, standardCredentials.getUsername(), standardCredentials.getPassword().getPlainText());
                testQuality.getList("milestone", "M", items, savedMilestone, project);
            } catch (JSONException | IOException | HttpException e) {
                LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
                //Don't think this does anything throw FormValidation.error("Connection error : " + e.getMessage(), e);
            }
            return items;
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         * @param project Project.
         * @param value File mask to validate.
         *
         * @return the validation result.
         * @throws IOException if an error occurs.
         */
        public FormValidation doCheckTestResults(
                @AncestorInPath AbstractProject project,
                @QueryParameter String value) throws IOException {
            if (project == null) {
                return FormValidation.ok();
            }
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

    }
}
