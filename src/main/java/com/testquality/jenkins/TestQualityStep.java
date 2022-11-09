package com.testquality.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.kohsuke.stapler.DataBoundSetter;

public class TestQualityStep extends Step {
    private final String project;
    private String cycle;
    private String milestone;
    private final String testResults;

    @DataBoundConstructor
    public TestQualityStep(String project, String testResults) {
        this.project = project;
        this.testResults = testResults;
    }

    @DataBoundSetter public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    @DataBoundSetter public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getProject() {
        return project;
    }

    public String getCycle() {
        return cycle;
    }

    public String getMilestone() {
        return milestone;
    }

    public String getTestResults() {
        return testResults;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends StepDescriptor implements FormValidationDelegator {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(TaskListener.class, FilePath.class, Run.class));
        }

        @Override
        public String getFunctionName() {
            return "testQuality";
        }

        @Override
        public String getDisplayName() {
            return "Upload test results to TestQuality";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Boolean> {

        protected transient TestQualityStep step;

        public Execution(TestQualityStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {

            TaskListener listener = getContext().get(TaskListener.class);
            FilePath workspace = getContext().get(FilePath.class);
            Run run = getContext().get(Run.class);

            Objects.requireNonNull(listener, "Task listener can not be null");

            TestResultsUploader resultsUploader = new TestResultsUploader(
                    step.getCycle(),
                    step.getMilestone(),
                    step.testResults,
                    step.project
            );

            return resultsUploader.upload(listener, run, workspace);
        }
    }

}
