/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
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

import java.security.cert.X509Certificate;
import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import org.onap.aai.babel.config.BabelAuthConfig;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.cl.api.Logger;

/**
 * Public class for authentication and authorization operations. Authorization is applied according to user and role
 */
public class AAIMicroServiceAuth {

    private static final Logger applicationLogger = LogHelper.INSTANCE;

    private BabelAuthConfig babelAuthConfig;

    /**
     * @param babelAuthConfig
     * @throws AAIAuthException
     */
    @Inject
    public AAIMicroServiceAuth(final BabelAuthConfig babelAuthConfig) throws AAIAuthException {
        this.babelAuthConfig = babelAuthConfig;
        if (!babelAuthConfig.isAuthenticationDisable()) {
            AAIMicroServiceAuthCore.init(babelAuthConfig.getAuthPolicyFile());
        }
    }

    /**
     * @param username
     * @param policyFunction
     * @return
     * @throws AAIAuthException
     */
    public boolean authorize(String username, String policyFunction) throws AAIAuthException {
        return AAIMicroServiceAuthCore.authorize(username, policyFunction);
    }

    /**
     * @param authUser
     * @param policyFunction
     * @return
     * @throws AAIAuthException
     */
    public String authenticate(String authUser, String policyFunction) throws AAIAuthException {
        if (authorize(authUser, policyFunction)) {
            return "OK";
        } else {
            return "AAI_9101";
        }
    }

    /**
     * @param headers
     * @param req
     * @param action
     * @param apiPath
     * @return
     * @throws AAIAuthException
     */
    public boolean validateRequest(HttpHeaders headers /* NOSONAR */, HttpServletRequest req,
            AAIMicroServiceAuthCore.HTTP_METHODS action, String apiPath) throws AAIAuthException {

        applicationLogger.debug("validateRequest: " + apiPath);
        applicationLogger
                .debug("babelAuthConfig.isAuthenticationDisable(): " + babelAuthConfig.isAuthenticationDisable());

        if (babelAuthConfig.isAuthenticationDisable()) {
            return true;
        }

        String[] ps = apiPath.split("/");
        String authPolicyFunctionName = ps[0];
        if (ps.length > 1 && authPolicyFunctionName.matches("v\\d+")) {
            authPolicyFunctionName = ps[1];
        }

        String cipherSuite = (String) req.getAttribute("javax.servlet.request.cipher_suite");
        String authUser = null;

        if (cipherSuite != null) {
            X509Certificate[] certChain = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
            X509Certificate clientCert = certChain[0];
            X500Principal subjectDN = clientCert.getSubjectX500Principal();
            authUser = subjectDN.toString();
        }

        if (authUser != null) {
            return "OK".equals(authenticate(authUser.toLowerCase(), action.toString() + ":" + authPolicyFunctionName));
        } else {
            return false;
        }
    }
}
