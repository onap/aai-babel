/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2018 European Software Marketing Ltd.
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

package org.onap.aai.babel.config;

import org.springframework.beans.factory.annotation.Value;

public class BabelAuthConfig {

    @Value("${auth.authentication.disable}")
    private boolean authenticationDisable;

    @Value("${auth.policy.file}")
    private String authPolicyFile;

    public boolean isAuthenticationDisable() {
        return authenticationDisable;
    }

    public void setAuthenticationDisable(boolean authenticationDisable) {
        this.authenticationDisable = authenticationDisable;
    }

    public String getAuthPolicyFile() {
        return authPolicyFile;
    }

    public void setAuthPolicyFile(String authPolicyFile) {
        this.authPolicyFile = authPolicyFile;
    }
}
