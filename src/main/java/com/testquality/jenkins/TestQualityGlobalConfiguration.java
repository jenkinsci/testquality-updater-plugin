package com.testquality.jenkins;

import com.testquality.jenkins.credentials.TestQualityBasicCredentials;
import com.testquality.jenkins.exception.ClientException;
import com.testquality.jenkins.exception.CredentialsException;
import hudson.Extension;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@Extension
public class TestQualityGlobalConfiguration extends jenkins.model.GlobalConfiguration {
    private static final String PLUGIN_SHORTNAME = "testquality-updater";
    private static final String DEFAULT_URL = "https://api.testquality.com";
    private static final String DISPLAY_NAME = "TestQuality Updater";
    private String url = DEFAULT_URL;
    private String username;
    private String password;

    @DataBoundConstructor
    public TestQualityGlobalConfiguration() {
        load();
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
            @QueryParameter("username") String username,
            @QueryParameter("password") String password
    ) {

        try {
            TestQualityClientFactory.create(
                    url, () -> new TestQualityBasicCredentials(username, password)
            );
            return FormValidation.ok("Successful Connection");
        } catch (CredentialsException ce) {
            return FormValidation.error(ce.getMessage());
        } catch (ClientException e) {
            return FormValidation.error("Connection error : " + e.getMessage());
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        this.url = formData.getString("url");
        this.username = formData.getString("username");
        this.password = formData.getString("password");
        return super.configure(req,formData);
    }

    public static TestQualityGlobalConfiguration get() {
        return all().get(TestQualityGlobalConfiguration.class);
    }

    public boolean isCredentialsExist() {
        return isNotBlank(url) && isNotBlank(username) && isNotBlank(password);
    }

}
