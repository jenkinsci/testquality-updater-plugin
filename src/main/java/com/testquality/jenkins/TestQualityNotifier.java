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
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
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
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> implements FormValidationDelegator {
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
                
    }
}
