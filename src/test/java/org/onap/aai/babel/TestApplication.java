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
    }

    @Test
    public void testApplicationStarts() {
        assertDoesNotThrow(() -> {
            BabelApplication.main(new String[]{});
            BabelApplication.exit();
        });
    }

    @Test
    public void testApplicationStartsWithObfuscatedPassword() {
        assertDoesNotThrow(() -> {
            BabelApplication.main(new String[]{});
            BabelApplication.exit();
        });
    }

    @Test
    public void testApplicationWithNullArgs() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            BabelApplication.main(null);
        });
        assertTrue(exception.getMessage().contains("Args must not be null"));
    }

}
