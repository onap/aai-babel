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
package org.onap.aai.babel.xml.generator.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerationData {

    List<Artifact> resultData = new ArrayList<>();
    Map<String, List<String>> errorData = new HashMap<>();

    public void add(List<Artifact> resultData, Map<String, List<String>> errorData) {
        this.resultData.addAll(resultData);
        this.errorData.putAll(errorData);
    }

    public void add(Artifact generatedArtifact) {
        resultData.add(generatedArtifact);
    }

    /**
     * Add the error code to the list of error codes for the given ID
     *
     * @param generatorId the generator id
     * @param errorCode the error code
     */
    public void add(String generatorId, String errorCode) {
        errorData.computeIfAbsent(generatorId, k -> new ArrayList<>());
        errorData.get(generatorId).add(errorCode);
    }

    public void add(GenerationData generationData) {
        this.resultData.addAll(generationData.resultData);
        this.errorData.putAll(generationData.errorData);
    }

    public List<Artifact> getResultData() {
        return resultData;
    }

    public Map<String, List<String>> getErrorData() {
        return errorData;
    }
}
