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

package org.onap.aai.babel.xml.generator.api;

import java.util.List;
import java.util.Map;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.GenerationData;

/**
 * Artifact Generation. Note that there is only one implementation of this interface currently.
 *
 */
@FunctionalInterface // for SONAR only
public interface ArtifactGenerator {

    /**
     * Implementation of the method to generate AAI artifacts.
     *
     * @param csarArchive
     *            original CSAR (zip format)
     * @param input
     *            List of input tosca files
     * @param additionalParams
     * @return Translated/Error data as a {@link GenerationData} object
     */
    public GenerationData generateArtifact(byte[] csarArchive, List<Artifact> input,
            Map<String, String> additionalParams);
}
