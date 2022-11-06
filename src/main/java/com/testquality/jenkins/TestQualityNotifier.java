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

import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.HttpException;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new AbortException("no workspace for " + build);
        }

        TestResultsUploader resultsUploader = new TestResultsUploader(plan, milestone, testResults, project);

        return resultsUploader.upload(listener, build, workspace);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public static final String NO_CONNECTION = "Please fill in connection details in Manage Jenkins -> Configure System";
        public static final String DISPLAY_NAME = "TestQuality Updater";
        private static final Logger LOGGER = Logger.getLogger("TestQualityPlugin.log");
        
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
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
        
        public FormValidation doCheckProject(@QueryParameter("project") String project) {
            TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
            if (!configuration.isCredentialsExist()) {
                    return FormValidation.error(NO_CONNECTION);
            }
            try {
                TestQualityClientFactory.create();
            } catch (ClientException e) {
                return FormValidation.error("Connection error : " + e.getMessage());
            }
            return FormValidation.ok();
        }
        
        public ListBoxModel doFillProjectItems(@QueryParameter("project") String savedProject) {
            ListBoxModel items = new ListBoxModel();
            TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();
            if (!configuration.isCredentialsExist()) {
                    return items;
            }
            
            if (StringUtils.isBlank(savedProject)) {
                items.add(new ListBoxModel.Option("", "", true));
            }

            try {
                HttpTestQuality testQuality = TestQualityClientFactory.create();
                testQuality.getList("project", "PJ", items, savedProject, "");
            } catch (JSONException | IOException | HttpException | ClientException e) {
                LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
                //Don't think this does anything throw FormValidation.error("Connection error : " + e.getMessage(), e);
            }
            return items;
	}
        
        public ListBoxModel doFillPlanItems(
			@QueryParameter String project,
                        @QueryParameter("plan") String savedPlan) {
            ListBoxModel items = new ListBoxModel();

            if (StringUtils.isBlank(project)
                    || project.trim().equals("-1")) {
                return items;
            }
            try {
                HttpTestQuality testQuality = TestQualityClientFactory.create();
                testQuality.getList("plan", "P", items, savedPlan, project);
            } catch (JSONException | IOException | HttpException | ClientException e) {
                LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
                //Don't think this does anything throw FormValidation.error("Connection error : " + e.getMessage(), e);
            }
            return items;
        }
        
        public ListBoxModel doFillMilestoneItems(
			@QueryParameter String project,
                        @QueryParameter("milestone") String savedMilestone) {
            ListBoxModel items = new ListBoxModel();

            if (StringUtils.isBlank(project)
                    || project.trim().equals(NO_CONNECTION)
                    || project.trim().equals("-1")) {
                return items;
            }
            
            items.add("Optionally Pick Milestone", "-1");
            
            try {
                HttpTestQuality testQuality = TestQualityClientFactory.create();
                testQuality.getList("milestone", "M", items, savedMilestone, project);
            } catch (JSONException | IOException | HttpException | ClientException e) {
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
