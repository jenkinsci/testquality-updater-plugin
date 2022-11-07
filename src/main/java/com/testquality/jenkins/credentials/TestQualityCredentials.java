package com.testquality.jenkins.credentials;

import com.testquality.jenkins.exception.CredentialsException;
import org.apache.commons.lang.StringUtils;

public interface TestQualityCredentials {

    String getUsername();

    String getPassword();

    default void validateInputString(String str, String cause) throws CredentialsException {
        if (StringUtils.isEmpty(str)) {
            throw new CredentialsException(cause);
        }
    }

}
