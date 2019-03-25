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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore;
import org.onap.aai.babel.config.BabelAuthConfig;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests {@link AAIMicroServiceAuth}.
 */

public class TestMicroServiceAuth {

    private static final String VALID_ADMIN_USER = "cn=common-name, ou=org-unit, o=org, l=location, st=state, c=us";
    private static final String TEST_POLICY_FILE = "auth_policy.json";

    @Before
    public void setup() {
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    /**
     * Test authorization of a request when authentication is disabled.
     *
     * @throws AAIAuthException
     *             if the test creates invalid Auth Policy roles
     */
    @Test
    public void testAuthenticationDisabled() throws AAIAuthException {
        BabelAuthConfig babelAuthConfig = new BabelAuthConfig();
        babelAuthConfig.setAuthenticationDisable(true);
        AAIMicroServiceAuth auth = new AAIMicroServiceAuth(babelAuthConfig);
        assertThat(auth.validateRequest(null, new MockHttpServletRequest(), null, "any/uri"), is(true));
    }

    /**
     * Temporarily invalidate the default policy file and then try to initialize the authorization class using the name
     * of a policy file that does not exist.
     *
     * @throws AAIAuthException
     *             if the Auth policy file cannot be loaded
     */
    @Test(expected = AAIAuthException.class)
    public void missingPolicyFile() throws AAIAuthException {
        String defaultFile = AAIMicroServiceAuthCore.getDefaultAuthFileName();
        try {
            AAIMicroServiceAuthCore.setDefaultAuthFileName("invalid.default.file");
            BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
            babelServiceAuthConfig.setAuthPolicyFile("invalid.file.name");
            new AAIMicroServiceAuth(babelServiceAuthConfig);
        } finally {
            AAIMicroServiceAuthCore.setDefaultAuthFileName(defaultFile);
        }
    }

    /**
     * Temporarily invalidate the default policy file and then try to initialize the authorization class using a null
     * policy file name.
     *
     * @throws AAIAuthException
     *             if the Auth policy file cannot be loaded
     */
    @Test(expected = AAIAuthException.class)
    public void testNullPolicyFile() throws AAIAuthException {
        String defaultFile = AAIMicroServiceAuthCore.getDefaultAuthFileName();
        try {
            AAIMicroServiceAuthCore.setDefaultAuthFileName("invalid.default.file");
            BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
            babelServiceAuthConfig.setAuthPolicyFile(null);
            new AAIMicroServiceAuth(babelServiceAuthConfig);
        } finally {
            AAIMicroServiceAuthCore.setDefaultAuthFileName(defaultFile);
        }
    }

    /**
     * Test reloading of the auth policy file after this file has been deleted.
     *
     * @throws AAIAuthException
     *             if the test creates invalid Auth Policy roles
     * @throws JSONException
     *             if this test creates an invalid JSON object
     * @throws IOException
     *             for I/O failures, e.g. when creating the temporary auth policy file
     */
    @Test(expected = AAIAuthException.class)
    public void testReloadDeletedFile() throws AAIAuthException, JSONException, IOException {
        File file = createTestPolicyFile();
        AAIMicroServiceAuthCore.init(file.getAbsolutePath());
        assertThat(file.delete(), is(true));
        AAIMicroServiceAuthCore.reloadUsers();
    }

    /**
     * Test reloading of the auth policy file after this file has been made invalid.
     *
     * @throws AAIAuthException
     *             if the test creates invalid Auth Policy roles
     * @throws JSONException
     *             if this test creates an invalid JSON object
     * @throws IOException
     *             for I/O failures, e.g. when creating the temporary auth policy file
     */
    @Test(expected = AAIAuthException.class)
    public void testReloadInvalidFile() throws AAIAuthException, JSONException, IOException {
        File file = createTestPolicyFile();
        AAIMicroServiceAuthCore.init(file.getAbsolutePath());
        writeToFile(file, "not valid JSON content");
        AAIMicroServiceAuthCore.reloadUsers();
    }

    /**
     * Test loading of a temporary file created with the specified roles.
     *
     * @throws AAIAuthException
     *             if the test creates invalid Auth Policy roles
     * @throws IOException
     *             for I/O failures
     * @throws JSONException
     *             if this test creates an invalid JSON object
     */
    @Test
    public void createLocalAuthFile() throws JSONException, AAIAuthException, IOException {
        createAuthService();
        assertThat(AAIMicroServiceAuthCore.authorize("nosuchuser", "method:func"), is(false));
        assertThat(AAIMicroServiceAuthCore.authorize("user", "method:func"), is(true));
    }

    /**
     * Test re-loading of users by changing the contents of a temporary file.
     *
     * @throws JSONException
     *             if this test creates an invalid JSON object
     * @throws AAIAuthException
     *             if the test creates invalid Auth Policy roles
     * @throws IOException
     *             for I/O failures
     * @throws InterruptedException
     *             if interrupted while sleeping
     */
    @Test
    public void createLocalAuthFileOnChange()
            throws JSONException, AAIAuthException, IOException, InterruptedException {
        File file = createTestPolicyFile();

        BabelAuthConfig babelAuthConfig = new BabelAuthConfig();
        babelAuthConfig.setAuthPolicyFile(file.getAbsolutePath());
        new AAIMicroServiceAuth(babelAuthConfig);

        // Make changes to the temp file
        writeToFile(file, "");

        // Wait for the file to be reloaded
        TimeUnit.SECONDS.sleep(3);

        AAIMicroServiceAuthCore.cleanup();
    }

    /**
     * Test that the default policy file is loaded when a non-existent file is passed to the authorization class.
     *
     * @throws AAIAuthException
     *             if the Auth Policy cannot be loaded
     */
    @Test
    public void createAuthFromDefaultFile() throws AAIAuthException {
        BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
        babelServiceAuthConfig.setAuthPolicyFile("non-existent-file");
        AAIMicroServiceAuth auth = new AAIMicroServiceAuth(babelServiceAuthConfig);
        // The default policy will have been loaded
        assertAdminUserAuthorisation(auth, VALID_ADMIN_USER);
    }

    /**
     * Test that the default policy file is loaded when a non-existent file is passed to the authorisation class and
     * CONFIG_HOME is not set.
     *
     * @throws AAIAuthException
     *             if the Auth Policy cannot be loaded
     */
    @Test
    public void createAuthFromDefaultFileAppHome() throws AAIAuthException {
        System.clearProperty("CONFIG_HOME");
        System.setProperty("APP_HOME", "src/test/resources");
        BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
        babelServiceAuthConfig.setAuthPolicyFile("non-existent-file");
        new AAIMicroServiceAuth(babelServiceAuthConfig);
        // The default policy will have been loaded from APP_HOME/appconfig
    }

    /**
     * Test loading of the policy file relative to CONFIG_HOME.
     *
     * @throws AAIAuthException
     *             if the Auth Policy cannot be loaded
     */
    @Test
    public void createAuth() throws AAIAuthException {
        AAIMicroServiceAuth auth = createStandardAuth();
        assertAdminUserAuthorisation(auth, VALID_ADMIN_USER);
    }

    @Test
    public void testAuthUser() throws AAIAuthException {
        createStandardAuth();
        assertThat(AAIMicroServiceAuthCore.authorize(VALID_ADMIN_USER, "GET:actions"), is(true));
        assertThat(AAIMicroServiceAuthCore.authorize(VALID_ADMIN_USER, "WRONG:action"), is(false));
    }

    @Test
    public void testValidateRequest() throws AAIAuthException {
        AAIMicroServiceAuth auth = createStandardAuth();
        assertThat(auth.validateRequest(null, new MockHttpServletRequest(), null, "app/v1/babel"), is(false));
    }

    private AAIMicroServiceAuth createStandardAuth() throws AAIAuthException {
        BabelAuthConfig babelServiceAuthConfig = new BabelAuthConfig();
        babelServiceAuthConfig.setAuthPolicyFile(TEST_POLICY_FILE);
        return new AAIMicroServiceAuth(babelServiceAuthConfig);
    }


    /**
     * Create a temporary JSON file using some valid test roles.
     *
     * @return the new temporary file
     * @throws IOException
     *             for I/O errors
     */
    private File createTestPolicyFile() throws JSONException, IOException {
        return createTempPolicyFile(createRoleObject("role", createUserObject("user"), createFunctionObject("func")));
    }

    /**
     * Create a test Auth policy JSON file and pass this to the Auth Service.
     *
     * @param roles
     *            the Auth policy JSON content
     * @return a new Auth Service configured with the supplied roles
     * @throws IOException
     *             for I/O failures
     * @throws AAIAuthException
     *             if the auth policy file cannot be loaded
     * @throws JSONException
     *             if this test creates an invalid JSON object
     */
    private AAIMicroServiceAuth createAuthService() throws AAIAuthException, IOException, JSONException {
        File file = createTestPolicyFile();
        BabelAuthConfig babelAuthConfig = new BabelAuthConfig();
        babelAuthConfig.setAuthPolicyFile(file.getAbsolutePath());
        return new AAIMicroServiceAuth(babelAuthConfig);
    }

    /**
     * Create a temporary JSON file using the supplied roles.
     *
     * @param roles
     *            the roles to use to populate the new file
     * @return the new temporary file
     * @throws IOException
     *             for I/O errors
     */
    private File createTempPolicyFile(JSONObject roles) throws IOException {
        File file = File.createTempFile("auth-policy", "json");
        file.deleteOnExit();
        writeToFile(file, roles.toString());
        return file;
    }

    /**
     * Write String content to a file, flush and close.
     *
     * @param file
     *            the file to write to
     * @param text
     *            the String content to write to the file
     * @throws IOException
     *             if the file cannot be opened for writing, or an I/O error occurs
     */
    private void writeToFile(File file, String text) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(text);
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Assert authorization results for an admin user based on the test policy file.
     *
     * @param auth
     *            the Auth Service to test
     * @param adminUser
     *            admin username
     * @throws AAIAuthException
     *             if the Auth Service is not initialized
     */
    private void assertAdminUserAuthorisation(AAIMicroServiceAuth auth, String adminUser) throws AAIAuthException {
        assertThat(AAIMicroServiceAuthCore.authorize(adminUser, "GET:actions"), is(true));
        assertThat(AAIMicroServiceAuthCore.authorize(adminUser, "POST:actions"), is(true));
        assertThat(AAIMicroServiceAuthCore.authorize(adminUser, "PUT:actions"), is(true));
        assertThat(AAIMicroServiceAuthCore.authorize(adminUser, "DELETE:actions"), is(true));
    }

    private JSONArray createFunctionObject(String functionName) throws JSONException {
        JSONArray functionsArray = new JSONArray();
        JSONObject func = new JSONObject();
        func.put("name", functionName);
        func.put("methods", createMethodObject("method"));
        functionsArray.put(func);
        return functionsArray;
    }

    private JSONArray createMethodObject(String methodName) throws JSONException {
        JSONArray methodsArray = new JSONArray();
        JSONObject method = new JSONObject();
        method.put("name", methodName);
        methodsArray.put(method);
        return methodsArray;
    }

    private JSONArray createUserObject(String username) throws JSONException {
        JSONArray usersArray = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("username", username);
        usersArray.put(user);
        return usersArray;
    }

    private JSONObject createRoleObject(String roleName, JSONArray usersArray, JSONArray functionsArray)
            throws JSONException {
        JSONObject role = new JSONObject();
        role.put("name", roleName);
        role.put("functions", functionsArray);
        role.put("users", usersArray);

        JSONArray rolesArray = new JSONArray();
        rolesArray.put(role);

        JSONObject roles = new JSONObject();
        roles.put("roles", rolesArray);
        return roles;
    }

}
