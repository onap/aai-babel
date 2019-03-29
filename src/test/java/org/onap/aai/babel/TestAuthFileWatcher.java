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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore;
import org.onap.aai.auth.AuthFileWatcher;
import org.onap.aai.babel.config.BabelAuthConfig;

/**
 * Tests {@link AuthFileWatcher}.
 */

public class TestAuthFileWatcher {

    private TimerTask task;
    private File mockFile = Mockito.mock(File.class);

    @Before
    public void createTask() {
        task = new AuthFileWatcher(mockFile);
    }

    @Test
    public void testOnChangeDoesNotRun() {
        task.run();
    }

    @Test
    public void testOnChangeDoesRun() throws IOException, AAIAuthException {
        System.setProperty("CONFIG_HOME", "src/test/resources");
        BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
        babelServiceAuthConfig.setAuthPolicyFile("auth_policy.json");
        new AAIMicroServiceAuth(babelServiceAuthConfig);

        Mockito.when(mockFile.lastModified()).thenReturn(1000L);
        task.run();
    }

    @Test
    public void testOnChangeRunAfterFailure() throws IOException {
        File file = File.createTempFile("auth-policy", "json");
        try {
            AAIMicroServiceAuthCore.init(file.getAbsolutePath());
        } catch (AAIAuthException e) {
            assertThat(e.getMessage(), containsString("Error processing Auth policy file"));
        }
        Mockito.when(mockFile.lastModified()).thenReturn(1000L);
        task.run();
    }

}
