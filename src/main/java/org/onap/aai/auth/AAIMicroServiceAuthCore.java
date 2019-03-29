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

package org.onap.aai.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;

/** Authentication and authorization by user and role. */
public class AAIMicroServiceAuthCore {

    private static LogHelper applicationLogger = LogHelper.INSTANCE;

    /**
     * The default policy file is expected to be located in either one of
     * <ul>
     * <li><code>$CONFIG_HOME/auth_policy.json</code></li>
     * <li><code>$CONFIG_HOME/auth/auth_policy.json</code></li>
     * <p>
     * Note that if <code>CONFIG_HOME</code> is not set then assume it has a value of <code>$APP_HOME/appconfig</code>
     */
    private static String defaultAuthFileName = "auth_policy.json";

    private static boolean usersInitialized = false;
    private static HashMap<String, AAIAuthUser> users;
    private static boolean timerSet = false;
    private static Timer timer = null;
    private static String policyAuthFileName;

    public enum HTTP_METHODS {
        GET, PUT, DELETE, HEAD, POST
    }

    // Don't instantiate
    private AAIMicroServiceAuthCore() {}

    public static String getDefaultAuthFileName() {
        return defaultAuthFileName;
    }

    public static void setDefaultAuthFileName(String defaultAuthFileName) {
        AAIMicroServiceAuthCore.defaultAuthFileName = defaultAuthFileName;
    }

    public static synchronized void init(String authPolicyFile) throws AAIAuthException {
        try {
            policyAuthFileName = AAIMicroServiceAuthCore.getConfigFile(authPolicyFile);
        } catch (IOException e) {
            applicationLogger.debug("Exception while retrieving policy file.");
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
            throw new AAIAuthException(e.getMessage());
        }

        if (policyAuthFileName == null) {
            throw new AAIAuthException("Auth policy file could not be found");
        }
        AAIMicroServiceAuthCore.reloadUsers();


        TimerTask task = new AuthFileWatcher(new File(policyAuthFileName));

        if (!timerSet) {
            timerSet = true;
            timer = new Timer();
            long period = TimeUnit.SECONDS.toMillis(1);
            timer.schedule(task, new Date(), period);
            applicationLogger.debug("Config Watcher Interval = " + period);
        }
    }

    public static void cleanup() {
        timer.cancel();
    }

    public static String getConfigFile(String authPolicyFile) throws IOException {
        return locateConfigFile(authPolicyFile).orElse(locateConfigFile(defaultAuthFileName).orElse(null));
    }

    /**
     * Locate the auth policy file by its name or path.
     * <ul>
     * <li>First try to use the absolute path to the file (if provided), or instead locate the path relative to the
     * current (or user) dir.</li>
     * <li>If this fails, try resolving the path relative to the configuration home location (either
     * <code>$CONFIG_HOME</code> or <code>$APP_HOME/appconfig</code>).</li>
     * <li>If this fails try resolving relative to the <code>auth</code> folder under configuration home.</li>
     *
     * @param authPolicyFile
     *            filename or path
     * @return the Optional canonical path to the located policy file
     * @throws IOException
     *             if the construction of the canonical pathname requires filesystem queries which cause I/O error(s)
     */
    private static Optional<String> locateConfigFile(String authPolicyFile) throws IOException {
        if (authPolicyFile != null) {
            List<Path> paths = new ArrayList<>();
            paths.add(Paths.get("."));

            String configHome = System.getProperty("CONFIG_HOME");
            if (configHome == null) {
                configHome = System.getProperty("APP_HOME") + "/appconfig";
            }

            paths.add(Paths.get(configHome));
            paths.add(Paths.get(configHome).resolve("auth"));

            for (Path path : paths) {
                File authFile = path.resolve(authPolicyFile).toFile();
                if (authFile.exists()) {
                    return Optional.of(authFile.getCanonicalPath());
                }
            }
        }

        return Optional.empty();
    }

    public static synchronized void reloadUsers() throws AAIAuthException {
        users = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            applicationLogger.debug("Reading from " + policyAuthFileName);
            JsonNode rootNode = mapper.readTree(new File(policyAuthFileName));
            for (JsonNode roleNode : rootNode.path("roles")) {
                String roleName = roleNode.path("name").asText();
                AAIAuthRole r = new AAIAuthRole();
                installFunctionOnRole(roleNode.path("functions"), roleName, r);
                assignRoleToUsers(roleNode.path("users"), roleName, r);
            }
        } catch (FileNotFoundException e) {
            throw new AAIAuthException("Auth policy file could not be found", e);
        } catch (JsonProcessingException e) {
            throw new AAIAuthException("Error processing Auth policy file ", e);
        } catch (IOException e) {
            throw new AAIAuthException("Error reading Auth policy file", e);
        }

        usersInitialized = true;
    }

    private static void installFunctionOnRole(JsonNode functionsNode, String roleName, AAIAuthRole r) {
        for (JsonNode functionNode : functionsNode) {
            String function = functionNode.path("name").asText();
            JsonNode methodsNode = functionNode.path("methods");
            boolean hasMethods = false;
            for (JsonNode method_node : methodsNode) {
                String methodName = method_node.path("name").asText();
                hasMethods = true;
                String func = methodName + ":" + function;
                applicationLogger.debug("Installing function " + func + " on role " + roleName);
                r.addAllowedFunction(func);
            }

            if (!hasMethods) {
                for (HTTP_METHODS meth : HTTP_METHODS.values()) {
                    String func = meth.toString() + ":" + function;
                    applicationLogger.debug("Installing (all methods) " + func + " on role " + roleName);
                    r.addAllowedFunction(func);
                }
            }
        }
    }

    private static void assignRoleToUsers(JsonNode usersNode, String roleName, AAIAuthRole r) {
        for (JsonNode userNode : usersNode) {
            String name = userNode.path("username").asText().toLowerCase();
            AAIAuthUser user;
            if (users.containsKey(name)) {
                user = users.get(name);
            } else {
                user = new AAIAuthUser();
            }
            applicationLogger.debug("Assigning " + roleName + " to user " + name);
            user.addRole(roleName, r);
            users.put(name, user);
        }
    }

    public static class AAIAuthUser {
        private HashMap<String, AAIAuthRole> roles;

        public AAIAuthUser() {
            this.roles = new HashMap<>();
        }

        public void addRole(String roleName, AAIAuthRole r) {
            this.roles.put(roleName, r);
        }

        public boolean checkAllowed(String checkFunc) {
            for (Entry<String, AAIAuthRole> role_entry : roles.entrySet()) {
                AAIAuthRole role = role_entry.getValue();
                if (role.hasAllowedFunction(checkFunc)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class AAIAuthRole {

        private List<String> allowedFunctions;

        public AAIAuthRole() {
            this.allowedFunctions = new ArrayList<>();
        }

        public void addAllowedFunction(String func) {
            this.allowedFunctions.add(func);
        }

        public boolean hasAllowedFunction(String afunc) {
            return this.allowedFunctions.contains(afunc);
        }
    }

    public static boolean authorize(String username, String authFunction) throws AAIAuthException {
        if (!usersInitialized || users == null) {
            throw new AAIAuthException("Auth module not initialized");
        }
        if (users.containsKey(username)) {
            if (users.get(username).checkAllowed(authFunction)) {
                logAuthenticationResult(username, authFunction, "AUTH ACCEPTED");
                return true;
            } else {
                logAuthenticationResult(username, authFunction, "AUTH FAILED");
                return false;
            }
        } else {
            logAuthenticationResult(username, authFunction, "User not found");
            return false;
        }
    }

    private static void logAuthenticationResult(String username, String authFunction, String result) {
        applicationLogger.debug(result + ": " + username + " on function " + authFunction);
    }
}
