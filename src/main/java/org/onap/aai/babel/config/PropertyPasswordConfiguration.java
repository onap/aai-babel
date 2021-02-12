/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.babel.config;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.security.Password;
import org.onap.aai.babel.logging.LogHelper;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyPasswordConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        Map<String, Object> sslProps = new LinkedHashMap<>();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String certPath = environment.getProperty("server.certs.location");
        File passwordFile = null;
        File passphrasesFile = null;
        InputStream passwordStream = null;
        InputStream passphrasesStream = null;
        String keystorePassword = null;
        String truststorePassword = null;

        if (certPath != null) {
            try {
                passwordFile = new File(certPath + ".password");
                passwordStream = new FileInputStream(passwordFile);

                if (passwordStream != null) {
                    keystorePassword = IOUtils.toString(passwordStream);
                    if (keystorePassword != null) {
                        keystorePassword = keystorePassword.trim();
                    }
                    sslProps.put("server.ssl.key-store-password", keystorePassword);
                }
            } catch (IOException e) {
            } finally {
                if (passwordStream != null) {
                    try {
                        passwordStream.close();
                    } catch (Exception e) {
                    }
                }
            }
            try {
                passphrasesFile = new File(certPath + ".passphrases");
                passphrasesStream = new FileInputStream(passphrasesFile);

                if (passphrasesStream != null) {
                    Properties passphrasesProps = new Properties();
                    passphrasesProps.load(passphrasesStream);
                    truststorePassword = passphrasesProps.getProperty("cadi_truststore_password");
                    if (truststorePassword != null) {
                        truststorePassword = truststorePassword.trim();
                    }
                    sslProps.put("server.ssl.trust-store-password", truststorePassword);
                } else {
                }
            } catch (IOException e) {
            } finally {
                if (passphrasesStream != null) {
                    try {
                        passphrasesStream.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
        if (keystorePassword == null || keystorePassword.isEmpty()) {
            keystorePassword = System.getProperty("KEY_STORE_PASSWORD");
            if (keystorePassword != null && (!keystorePassword.isEmpty()) ) {
                System.setProperty("server.ssl.key-store-password", new Password(keystorePassword).toString());
            }
            if (keystorePassword == null || keystorePassword.isEmpty()) {
                throw new IllegalArgumentException("Mandatory property KEY_STORE_PASSWORD not set");
            }
        }
        else {
            sslProps.put("server.ssl.key-store-password", keystorePassword);
        }
        if (truststorePassword == null || truststorePassword.isEmpty()) {
        }
        else {
            sslProps.put("server.ssl.trust-store-password", truststorePassword);
        }
        if (!sslProps.isEmpty()) {
            PropertySource<?> additionalProperties = new MapPropertySource("additionalProperties", sslProps);
            environment.getPropertySources().addFirst(additionalProperties);
        }
    }
}
