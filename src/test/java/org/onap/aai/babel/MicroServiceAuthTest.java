/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 European Software Marketing Ltd.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.babel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuth;
import org.onap.aai.auth.AAIMicroServiceAuthCore;
import org.onap.aai.babel.config.BabelAuthConfig;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests @{link AAIMicroServiceAuth}
 */

public class MicroServiceAuthTest {

    private static final String VALID_ADMIN_USER = "cn=common-name, ou=org-unit, o=org, l=location, st=state, c=us";
    private static final String authPolicyFile = "auth_policy.json";

    static {
        System.setProperty("CONFIG_HOME",
                System.getProperty("user.dir") + File.separator + "src/test/resources");
    }

    /**
     * Temporarily invalidate the default policy file and then try to initialise the authorisation class using the name
     * of a policy file that does not exist.
     * 
     * @throws AAIAuthException
     * @throws IOException
     */
    @Test(expected = AAIAuthException.class)
    public void missingPolicyFile() throws AAIAuthException, IOException {
        String defaultFile = AAIMicroServiceAuthCore.getDefaultAuthFileName();
        try {
            AAIMicroServiceAuthCore.setDefaultAuthFileName("invalid.default.file");
            BabelAuthConfig gapServiceAuthConfig = new BabelAuthConfig();
            gapServiceAuthConfig.setAuthPolicyFile("invalid.file.name");
            new AAIMicroServiceAuth(gapServiceAuthConfig);
        } finally {
            AAIMicroServiceAuthCore.setDefaultAuthFileName(defaultFile);
        }
    }

    /**
     * Test loading of a temporary file created with the specified roles
     * 
     * @throws AAIAuthException
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void createLocalAuthFile() throws AAIAuthException, IOException, JSONException {
        JSONObject roles = createRoleObject("role", createUserObject("user"), createFunctionObject("func"));
        AAIMicroServiceAuth auth = createAuthService(roles);
        assertThat(auth.authorize("nosuchuser", "method:func"), is(false));
        assertThat(auth.authorize("user", "method:func"), is(true));
    }

    /**
     * Test that the default policy file is loaded when a non-existent file is passed to the authorisation clas.
     * 
     * @throws AAIAuthException
     */
    @Test
    public void createAuthFromDefaultFile() throws AAIAuthException {
        BabelAuthConfig gapServiceAuthConfig = new BabelAuthConfig();
        gapServiceAuthConfig.setAuthPolicyFile("non-existent-file");
        AAIMicroServiceAuth auth = new AAIMicroServiceAuth(gapServiceAuthConfig);
        // The default policy will have been loaded
        assertAdminUserAuthorisation(auth, VALID_ADMIN_USER);
    }

    /**
     * Test loading of the policy file relative to CONFIG_HOME
     * 
     * @throws AAIAuthException
     */
    @Test
    public void createAuth() throws AAIAuthException {
        AAIMicroServiceAuth auth = createStandardAuth();
        assertAdminUserAuthorisation(auth, VALID_ADMIN_USER);
    }

    @Test
    public void testAuthUser() throws AAIAuthException {
        AAIMicroServiceAuth auth = createStandardAuth();
        assertThat(auth.authenticate(VALID_ADMIN_USER, "GET:actions"), is(equalTo("OK")));
        assertThat(auth.authenticate(VALID_ADMIN_USER, "WRONG:action"), is(equalTo("AAI_9101")));
    }



    @Test
    public void testValidateRequest() throws AAIAuthException {
        AAIMicroServiceAuth auth = createStandardAuth();
        assertThat(auth.validateRequest(null, new MockHttpServletRequest(), null, "app/v1/gap"), is(false));
    }

    private AAIMicroServiceAuth createStandardAuth() throws AAIAuthException {
        BabelAuthConfig gapServiceAuthConfig = new BabelAuthConfig();
        gapServiceAuthConfig.setAuthPolicyFile(authPolicyFile);
        return new AAIMicroServiceAuth(gapServiceAuthConfig);
    }

    /**
     * @param rolesJson
     * @return
     * @throws IOException
     * @throws AAIAuthException
     */
    private AAIMicroServiceAuth createAuthService(JSONObject roles) throws IOException, AAIAuthException {
        BabelAuthConfig babelAuthConfig = new BabelAuthConfig();
        File file = File.createTempFile("auth-policy", "json");
        file.deleteOnExit();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(roles.toString());
        fileWriter.flush();
        fileWriter.close();

        babelAuthConfig.setAuthPolicyFile(file.getAbsolutePath());
        return new AAIMicroServiceAuth(babelAuthConfig);
    }

    /**
     * Assert authorisation results for an admin user based on the test policy file
     * 
     * @param auth
     * @param adminUser
     * @throws AAIAuthException
     */
    private void assertAdminUserAuthorisation(AAIMicroServiceAuth auth, String adminUser) throws AAIAuthException {
        assertThat(auth.authorize(adminUser, "GET:actions"), is(true));
        assertThat(auth.authorize(adminUser, "POST:actions"), is(true));
        assertThat(auth.authorize(adminUser, "PUT:actions"), is(true));
        assertThat(auth.authorize(adminUser, "DELETE:actions"), is(true));
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
        JSONObject roles = new JSONObject();

        JSONObject role = new JSONObject();
        role.put("name", roleName);
        role.put("functions", functionsArray);
        role.put("users", usersArray);

        JSONArray rolesArray = new JSONArray();
        rolesArray.put(role);
        roles.put("roles", rolesArray);

        return roles;
    }

}
