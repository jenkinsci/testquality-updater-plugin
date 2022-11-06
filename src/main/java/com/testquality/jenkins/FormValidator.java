package com.testquality.jenkins;

import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

public interface FormValidator {
    FormValidation doCheckProject(@QueryParameter("project") String project);

    ListBoxModel doFillProjectItems(@QueryParameter("project") String savedProject);

    ListBoxModel doFillPlanItems(
            @QueryParameter String project,
            @QueryParameter("plan") String savedPlan
    );

    ListBoxModel doFillMilestoneItems(
            @QueryParameter String project,
            @QueryParameter("milestone") String savedMilestone
    );

    /**
     * Performs on-the-fly validation on the file mask wildcard.
     * @param project Project.
     * @param value File mask to validate.
     *
     * @return the validation result.
     * @throws IOException if an error occurs.
     */
    FormValidation doCheckTestResults(
            @AncestorInPath AbstractProject<?, ?> project,
            @QueryParameter String value
    ) throws IOException;
}
