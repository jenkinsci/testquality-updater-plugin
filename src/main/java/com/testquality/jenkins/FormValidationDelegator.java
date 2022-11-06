package com.testquality.jenkins;

import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

public interface FormValidationDelegator extends FormValidator {

    TestQualityFormValidator formValidator = new TestQualityFormValidator();

    @Override
    default FormValidation doCheckProject(@QueryParameter("project") String project) {
        return formValidator.doCheckProject(project);
    }

    @Override
    default ListBoxModel doFillProjectItems(@QueryParameter("project") String savedProject) {
        return formValidator.doFillProjectItems(savedProject);
    }

    @Override
    default ListBoxModel doFillPlanItems(
            @QueryParameter String project,
            @QueryParameter("plan") String savedPlan
    ) {
        return formValidator.doFillPlanItems(project, savedPlan);
    }

    @Override
    default ListBoxModel doFillMilestoneItems(
            @QueryParameter String project,
            @QueryParameter("milestone") String savedMilestone
    ) {
        return formValidator.doFillMilestoneItems(project, savedMilestone);
    }

    @Override
    default FormValidation doCheckTestResults(
            @AncestorInPath AbstractProject<?, ?> project,
            @QueryParameter String value
    ) throws IOException {
        return formValidator.doCheckTestResults(project, value);
    }
}
