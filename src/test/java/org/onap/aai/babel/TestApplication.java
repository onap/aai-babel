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

import java.io.IOException;
import org.eclipse.jetty.util.security.Password;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BabelApplication.class)
public class TestApplication {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * Initialize System Properties.
     */
    @Before
    public void init() {
        System.setProperty("APP_HOME", ".");
        System.setProperty("CONFIG_HOME", "src/test/resources");
        System.setProperty("server.ssl.key-store", "src/test/resources/auth/keystore.jks");
    }

    @Test
    public void testApplicationStarts() {
        System.setProperty("KEY_STORE_PASSWORD", "password");
        BabelApplication.main(new String[] {});
        BabelApplication.exit();
    }

    @Test
    public void testApplicationStartsWithObfuscatedPassword() {
        System.setProperty("KEY_STORE_PASSWORD", Password.obfuscate("password"));
        BabelApplication.main(new String[] {});
        BabelApplication.exit();
    }

    @Test
    public void testApplicationWithNullArgs() {
        System.setProperty("KEY_STORE_PASSWORD", "test");
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Args must not be null");
        BabelApplication.main(null);
    }

    @Test
    public void testApplicationWithEmptyKeyStorePassword() {
        System.setProperty("KEY_STORE_PASSWORD", "");
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("roperty KEY_STORE_PASSWORD not set");
        BabelApplication.main(new String[] {});
    }

    @Test
    public void testApplicationWithNullKeyStorePassword() {
        System.clearProperty("KEY_STORE_PASSWORD");
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("roperty KEY_STORE_PASSWORD not set");
        BabelApplication.main(new String[] {});
    }

    @Test
    public void testApplicationWithIncorrectKeyStorePassword() {
        System.setProperty("KEY_STORE_PASSWORD", "test");
        final CauseMatcher expectedCause = new CauseMatcher(IOException.class, "password was incorrect");
        expectedEx.expectCause(expectedCause);
        BabelApplication.main(new String[] {});
    }

    private static class CauseMatcher extends TypeSafeMatcher<Throwable> {

        private final Class<? extends Throwable> type;
        private final String expectedMessage;

        public CauseMatcher(Class<? extends Throwable> type, String expectedMessage) {
            this.type = type;
            this.expectedMessage = expectedMessage;
        }

        @Override
        protected boolean matchesSafely(Throwable item) {
            return item.getClass().isAssignableFrom(type) && item.getMessage().contains(expectedMessage);
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(type).appendText(" and message ").appendValue(expectedMessage);
        }
    }

}
