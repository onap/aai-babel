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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
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
    public void testApplicationWithAuthEnvVar() throws Exception {
        setEnv(Collections.singletonMap("REQUIRE_CLIENT_AUTH", "TRUE"));
        System.setProperty("KEY_STORE_PASSWORD", "password");
        BabelApplication.main(new String[] {});
        BabelApplication.exit();

        setEnv(Collections.singletonMap("REQUIRE_CLIENT_AUTH", "False"));
        BabelApplication.main(new String[] {});
        BabelApplication.exit();

        setEnv(Collections.singletonMap("REQUIRE_CLIENT_AUTH", "other"));
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

    /**
     * In commit 132d44f a change was made to read from the System environment with
     * <code>System.getenv("REQUIRE_CLIENT_AUTH")</code>
     * <p>
     * Given that the environment is set by the Operating System and is unmodifiable, the following is needed to alter
     * the Map values for the running process.
     * </p>
     *
     * @param newenv
     *            the new Environment key/value pairs
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @SuppressWarnings("unchecked")
    protected static void setEnv(Map<String, String> newenv) throws ClassNotFoundException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField =
                    processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
