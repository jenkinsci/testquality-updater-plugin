package com.testquality.jenkins;

import com.google.common.collect.ImmutableMap;
import com.testquality.jenkins.exception.CredentialsException;
import com.testquality.jenkins.exception.HttpException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Failure;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.*;

public class TestQualityStep extends Step {
    private final String project;
    private String plan;
    private String milestone;
    private final String testResults;

    @DataBoundConstructor
    public TestQualityStep(String project, String testResults) {
        this.project = project;
        this.testResults = testResults;
    }

    @DataBoundSetter public void setPlan(String plan) {
        this.plan = plan;
    }

    @DataBoundSetter public void setMilestone(String milestone) {
        this.milestone = milestone;
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

    // TODO: fix descriptor
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

            TestQualityClient client = TestQualityClientFactory.create();

            try {

                String projectId = getFirstIdByNameOrThrow(
                        client.projects(),
                        step.project,
                        String.format("Project with name {%s} doesn't exist", step.project)
                );

                String milestone = "-1";

                if (StringUtils.isNotEmpty(step.milestone)) {

                    Map<String, String> params = ImmutableMap.of("project_id", projectId);
                    milestone = getFirstIdByNameOrThrow(
                            client.milestones(params),
                            step.milestone,
                            String.format("Milestone with name {%s} doesn't exist", step.milestone)
                    );
                }

                String cycle = "-1";

                if (StringUtils.isNotEmpty(step.plan)) {

                    Map<String, String> params = ImmutableMap.of(
                            "project_id", projectId,
                            "is_root", "false"
                    );
                    cycle = getFirstIdByNameOrThrow(
                            client.cycles(params),
                            step.plan,
                            String.format("Cycles with name {%s} doesn't exist", step.plan)
                    );

                }

                TestResultsUploader resultsUploader = new TestResultsUploader(
                        cycle,
                        milestone,
                        step.testResults,
                        projectId
                );

                return resultsUploader.upload(listener, run, workspace);
            } catch (JSONException | IOException | HttpException | CredentialsException e) {
                throw new Failure(e.getMessage());
            }
        }

        private String getFirstIdByNameOrThrow(List<TestQualityBaseResponse> responses,
                                               String name,
                                               String cause) {

            return responses.stream()
                    .filter(resp -> resp.getName().equals(name))
                    .findFirst()
                    .map(TestQualityBaseResponse::getId)
                    .map(String::valueOf)
                    .orElseThrow(() -> new Failure(cause));
        }
    }

}
