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

public class TestQualityStep extends Step {
    private final String project;
    private final String plan;
    private final String milestone;
    private final String testResults;

    @DataBoundConstructor
    public TestQualityStep(String project, String plan, String milestone, String testResults) {
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
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends StepDescriptor {

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

    public static class Execution extends SynchronousNonBlockingStepExecution<Boolean> implements FormValidationDelegator {

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
                    step.getPlan(),
                    step.getMilestone(),
                    step.testResults,
                    step.project
            );

            return resultsUploader.upload(listener, run, workspace);
        }
    }

}
