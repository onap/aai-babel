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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.onap.aai.auth.AAIAuthException;
import org.onap.aai.auth.AAIMicroServiceAuthCore;

/**
 * Tests {@link AAIMicroServiceAuthCore}.
 */

public class TestMicroServiceAuthCore {

    /**
     * Test calling the authorize method without loading the auth policy.
     *
     * @throws AAIAuthException
     *             when the module has not been initialized
     */
    @Test
    public void testUninitializedModule() throws AAIAuthException {
        assertThrows(AAIAuthException.class, () -> {
            AAIMicroServiceAuthCore.authorize("user", "method:func");
        });
    }

}
