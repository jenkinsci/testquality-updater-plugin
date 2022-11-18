package com.testquality.jenkins;

import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.HttpException;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestQualityFormValidator implements FormValidator {

    private static final String NO_CONNECTION = "Please fill in connection details in Manage Jenkins -> Configure System";

    private static final Logger LOGGER = Logger.getLogger(TestQualityFormValidator.class.getName());

    @Override
    public FormValidation doCheckProject(@QueryParameter("project") String project) {
        TestQualityGlobalConfiguration configuration = TestQualityGlobalConfiguration.get();

        if (!configuration.isConfigured()) return FormValidation.error(NO_CONNECTION);

        try {
            TestQualityClientFactory.create();
        } catch (JSONException | HttpException | ClientException e) {
            return FormValidation.error("Connection error : " + e.getMessage());
        }
        return FormValidation.ok();
    }

    @Override
    public ListBoxModel doFillProjectItems(@QueryParameter("project") String savedProject) {

        ListBoxModel items = new ListBoxModel();

        if (StringUtils.isBlank(savedProject)) {
            items.add(new ListBoxModel.Option("", "", true));
        }

        return getItems("project", "PJ", items, savedProject, null);
    }

    @Override
    public ListBoxModel doFillPlanItems(@QueryParameter String project, @QueryParameter("plan") String savedPlan) {

        ListBoxModel items = new ListBoxModel();

        if (StringUtils.isBlank(project) || project.trim().equals("-1")) {
            return items;
        }

        Map<String, String> params = new HashMap<>();
        params.put("project_id", project);
        params.put("is_root", "false");

        items.add("Use Root Cycle", "-1");

        return getItems("plan", "P", items, savedPlan, params);
    }

    @Override
    public ListBoxModel doFillMilestoneItems(
            @QueryParameter String project,
            @QueryParameter("milestone") String savedMilestone)
    {
        ListBoxModel items = new ListBoxModel();

        if (StringUtils.isBlank(project) || project.trim().equals(NO_CONNECTION) || project.trim().equals("-1")) {
            return items;
        }

        items.add("Optionally Pick Milestone", "-1");

        return getItems("milestone", "M", items, savedMilestone, Collections.singletonMap("project_id", project));
    }

    @Override
    public FormValidation doCheckTestResults(
            @AncestorInPath AbstractProject<?, ?> project,
            @QueryParameter String value) throws IOException {
        if (project == null) {
            return FormValidation.ok();
        }
        return FilePath.validateFileMask(project.getSomeWorkspace(), value);
    }

    private ListBoxModel getItems(String type, String keyPrefix, ListBoxModel items, String id, Map<String, String> params) {

        if (!TestQualityGlobalConfiguration.get().isConfigured()) {
            return new ListBoxModel();
        }

        try {
            TestQualityClient testQuality = TestQualityClientFactory.create();
            testQuality.getList(type, keyPrefix, items, id, params);
        } catch (JSONException | IOException | HttpException e) {
            LOGGER.log(Level.SEVERE, "ERROR: Filling List Box, " + e.getMessage(), e);
        }
        return items;
    }

}
