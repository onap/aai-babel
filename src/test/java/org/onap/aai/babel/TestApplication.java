/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.babel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextException;

@SpringBootTest(classes = BabelApplication.class)
public class TestApplication {

    /**
     * Initialize System Properties.
     */
    @BeforeEach
    public void init() {
        System.setProperty("APP_HOME", ".");
        System.setProperty("CONFIG_HOME", "src/test/resources");
        System.setProperty("server.ssl.key-store", "src/test/resources/auth/keystore.jks");
    }

    @Test
    public void testApplicationStarts() {
        assertDoesNotThrow(() -> {
            System.setProperty("KEY_STORE_PASSWORD", "password");
            BabelApplication.main(new String[]{});
            BabelApplication.exit();
        });
    }

    @Test
    public void testApplicationStartsWithObfuscatedPassword() {
        assertDoesNotThrow(() -> {
            System.setProperty("KEY_STORE_PASSWORD", Password.obfuscate("password"));
            BabelApplication.main(new String[]{});
            BabelApplication.exit();
        });
    }

    @Test
    public void testApplicationWithNullArgs() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            System.setProperty("KEY_STORE_PASSWORD", "test");
            BabelApplication.main(null);
        });
        assertTrue(exception.getMessage().contains("Args must not be null"));
    }

    @Test
    public void testApplicationWithEmptyKeyStorePassword() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            System.setProperty("KEY_STORE_PASSWORD", "");
            BabelApplication.main(new String[]{});
        });
        assertTrue(exception.getMessage().contains("roperty KEY_STORE_PASSWORD not set"));
    }

    @Test
    public void testApplicationWithNullKeyStorePassword() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            System.clearProperty("KEY_STORE_PASSWORD");
            BabelApplication.main(new String[]{});
        });
        assertTrue(exception.getMessage().contains("roperty KEY_STORE_PASSWORD not set"));
    }

    @Test
    public void testApplicationWithIncorrectKeyStorePassword() {
        Throwable exception = assertThrows(ApplicationContextException.class, () -> {
            System.setProperty("KEY_STORE_PASSWORD", "test");
            BabelApplication.main(new String[]{});
        });
    }

    /**
     * This test asserts that if the KEY_STORE_PASSWORD System Property is set (and is not empty) then the value is
     * passed to Jetty, debobfuscated, and used to open the key store, even if the resulting password value is actually
     * an empty string.
     */
    @Test
    public void testApplicationWithBlankObfuscatedKeyStorePassword() {
        // Note that "OBF:" is correctly deobfuscated and results in an empty string.
        Throwable exception = assertThrows(ApplicationContextException.class, () -> {
            System.setProperty("KEY_STORE_PASSWORD", "OBF:");
            BabelApplication.main(new String[]{});
        });
    }

}
