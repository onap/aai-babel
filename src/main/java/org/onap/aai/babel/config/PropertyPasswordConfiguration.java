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

import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.util.security.Password;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyPasswordConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String PROP_KEY_STORE_PASS = "server.ssl.key-store-password";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        Map<String, Object> sslProps = new LinkedHashMap<>();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String certPath = environment.getProperty("server.certs.location");
        String keystorePassword = null;
        String truststorePassword = null;

        if (certPath != null) {
            try (InputStream passwordStream = new FileInputStream(certPath + ".password")) {
                keystorePassword = new String(passwordStream.readAllBytes(), StandardCharsets.UTF_8);
                keystorePassword = keystorePassword.trim();
                sslProps.put(PROP_KEY_STORE_PASS, keystorePassword);
            } catch (IOException e) {
                keystorePassword = null;
            }
            try (InputStream passphrasesStream = new FileInputStream(certPath + ".passphrases");) {
                Properties passphrasesProps = new Properties();
                passphrasesProps.load(passphrasesStream);
                truststorePassword = passphrasesProps.getProperty("cadi_truststore_password");
                if (truststorePassword != null) {
                    truststorePassword = truststorePassword.trim();
                }
                sslProps.put("server.ssl.trust-store-password", truststorePassword);
            } catch (IOException e) {
                truststorePassword = null;
            }
        }
        if (keystorePassword == null || keystorePassword.isEmpty()) {
            keystorePassword = System.getProperty("KEY_STORE_PASSWORD");
            if (keystorePassword != null && (!keystorePassword.isEmpty()) ) {
                System.setProperty(PROP_KEY_STORE_PASS, new Password(keystorePassword).toString());
            }
            if (keystorePassword == null || keystorePassword.isEmpty()) {
                throw new IllegalArgumentException("Mandatory property KEY_STORE_PASSWORD not set");
            }
        }
        else {
            sslProps.put(PROP_KEY_STORE_PASS, keystorePassword);
        }
        if (truststorePassword != null && !truststorePassword.isEmpty()) {
            sslProps.put("server.ssl.trust-store-password", truststorePassword);
        }
        if (!sslProps.isEmpty()) {
            PropertySource<?> additionalProperties = new MapPropertySource("additionalProperties", sslProps);
            environment.getPropertySources().addFirst(additionalProperties);
        }
    }
}
