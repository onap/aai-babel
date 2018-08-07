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

package org.onap.aai.babel.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import org.onap.sdc.toscaparser.api.NodeTemplate;

/**
 * Direct tests of the TOSCA parser-based Artifact Generator, to cover exceptional cases.
 */

public class TestArtifactGeneratorToscaParser {

    ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(null);

    /**
     * Process a dummy Node Template object for a Service. A WARNING should be logged for the missing metadata.
     */
    @Test
    public void testMissingServiceData() {
        List<NodeTemplate> nodeTemplateList = Collections.singletonList(buildNodeTemplate("name", "BlockStorage"));
        parser.processServiceTosca(null, Collections.emptyMap(), nodeTemplateList);
        parser.processResourceToscas(nodeTemplateList, null);
    }

    private NodeTemplate buildNodeTemplate(String name, String type) {
        LinkedHashMap<String, Object> nodeTemplateMap = new LinkedHashMap<>();
        LinkedHashMap<String, Object> template = new LinkedHashMap<>();
        template.put("type", type);
        nodeTemplateMap.put(name, template);
        return new NodeTemplate(name, nodeTemplateMap, null, null, null);
    }

}
