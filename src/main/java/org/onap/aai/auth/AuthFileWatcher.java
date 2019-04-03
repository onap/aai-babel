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

import java.io.File;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;

public class AuthFileWatcher extends FileWatcher {

    private static LogHelper applicationLogger = LogHelper.INSTANCE;

    public AuthFileWatcher(File file) {
        super(file);
    }

    @Override
    protected void onChange(File file) {
        applicationLogger.debug("File " + file.getName() + " has been changed!");
        try {
            AAIMicroServiceAuthCore.reloadUsers();
        } catch (AAIAuthException e) {
            applicationLogger.error(ApplicationMsgs.PROCESS_REQUEST_ERROR, e);
        }
        applicationLogger.debug("File " + file.getName() + " has been reloaded!");
    }
}
