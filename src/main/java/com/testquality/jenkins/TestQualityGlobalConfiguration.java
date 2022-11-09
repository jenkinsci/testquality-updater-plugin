package com.testquality.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.CredentialsException;
import hudson.Extension;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;


@Extension
public class TestQualityGlobalConfiguration extends GlobalConfiguration {
    private static final String PLUGIN_SHORTNAME = "testquality-updater";
    private static final String DEFAULT_URL = "https://api.testquality.com";
    private static final String DISPLAY_NAME = "TestQuality Updater";
    private String url = DEFAULT_URL;
    private String credentialsId;

    @DataBoundConstructor
    public TestQualityGlobalConfiguration() {
        load();
    }

    public String getUrl() {
        return url;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getHelpFile() {
        return "/plugin/" + PLUGIN_SHORTNAME + "/help/help.html";
    }

    public FormValidation doTestConnection(
            @QueryParameter("url") String url,
            @QueryParameter("credentialsId") String credentialsId
    ) {
        if (isBlank(credentialsId)) {
            return FormValidation.error("Credentials are not specified");
        }

        try {
            StandardUsernamePasswordCredentials standardCredentials = getCredentials(url, credentialsId);

            TestQualityClientFactory.create(url, standardCredentials);
            return FormValidation.ok("Successful Connection");
        } catch (CredentialsException ce) {
            return FormValidation.error(ce.getMessage());
        } catch (ClientException e) {
            return FormValidation.error("Connection error : " + e.getMessage());
        }
    }

    public ListBoxModel doFillCredentialsIdItems(@QueryParameter String url,
                                                @QueryParameter String credentialsId) {
        Job owner = null;

        List<DomainRequirement> apiEndpoint = URIRequirementBuilder.fromUri(url).build();

        return new StandardUsernameListBoxModel()
                .withEmptySelection()
                .withAll(CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, owner, null, apiEndpoint));
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        this.url = formData.getString("url");
        this.credentialsId = formData.getString("credentialsId");
        return super.configure(req,formData);
    }

    public static TestQualityGlobalConfiguration get() {
        return all().get(TestQualityGlobalConfiguration.class);
    }

    public StandardUsernamePasswordCredentials getCredentials() {
        return getCredentials(this.url, this.credentialsId);
    }

    public boolean isConfigured() {
        return isNotBlank(url) && isNotBlank(credentialsId);
    }
    private StandardUsernamePasswordCredentials getCredentials(String url, String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider
                .lookupCredentials(StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(), null,
                        fromUri(url).build()),
                CredentialsMatchers.withId(credentialsId));

    }

}
