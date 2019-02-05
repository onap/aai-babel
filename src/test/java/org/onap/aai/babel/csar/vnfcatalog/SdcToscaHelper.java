/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 European Software Marketing Ltd.
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

package org.onap.aai.babel.csar.vnfcatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.SubstitutionMappings;

public class SdcToscaHelper {

    private ArrayList<NodeTemplate> smnodetemplates = new ArrayList<>();

    /**
     * Create the test SubstitutionMappings.
     * 
     * @return the new Substitution Mappings
     */
    public SubstitutionMappings buildMappings() {
        LinkedHashMap<String, Object> defProps = getImagesDefProps();

        LinkedHashMap<String, Object> defs = buildNodeTemplateTypeInfo(defProps);
        LinkedHashMap<String, Object> caps = new LinkedHashMap<>();
        LinkedHashMap<String, Object> reqs = new LinkedHashMap<>();

        String type = "tosca.nodes.custom";

        LinkedHashMap<String, Object> smsubMappingDef = new LinkedHashMap<>();
        smsubMappingDef.put("node_type", type);
        smsubMappingDef.put("capabilities", caps);
        smsubMappingDef.put("requirements", reqs);

        LinkedHashMap<String, Object> smcustomDefs = buildCustomTypeDefinitions(type, defs);

        return new SubstitutionMappings(smsubMappingDef, smnodetemplates, null, null, null, null, smcustomDefs);
    }

    private LinkedHashMap<String, Object> getImagesDefProps() {
        LinkedHashMap<String, Object> imagesDef = new LinkedHashMap<>();
        imagesDef.put("type", "map");
        imagesDef.put("required", false);
        imagesDef.put("entry_schema", "{type=org.openecomp.datatypes.ImageInfo}");

        LinkedHashMap<String, Object> defProps = new LinkedHashMap<>();
        defProps.put("images", imagesDef);
        return defProps;
    }

    private LinkedHashMap<String, Object> buildCustomTypeDefinitions(String type,
            LinkedHashMap<String, Object> typeInfo) {
        LinkedHashMap<String, Object> customDefs = new LinkedHashMap<>();
        customDefs.put(type, typeInfo);
        return customDefs;
    }

    private LinkedHashMap<String, Object> buildNodeTemplateTypeInfo(LinkedHashMap<String, Object> props) {
        LinkedHashMap<String, Object> typeInfo = new LinkedHashMap<>();
        typeInfo.put("derived_from", "tosca.nodes.Root");
        typeInfo.put("properties", props);
        return typeInfo;
    }

    /**
     * Create a new NodeTemplate and add it to the list (for populating the Substitution Mappings).
     */
    public void addNodeTemplate() {
        String name = "node name";
        String type = "tosca.nodes.custom";

        LinkedHashMap<String, Object> nodeTemplate = new LinkedHashMap<>();
        nodeTemplate.put("type", type);
        nodeTemplate.put("properties", null);

        LinkedHashMap<String, Object> ntnodeTemplates = buildCustomTypeDefinitions(name, nodeTemplate);
        ntnodeTemplates.put("derived_from", null);
        ntnodeTemplates.put("properties", getImagesDefProps());

        LinkedHashMap<String, Object> typeInfo = buildNodeTemplateTypeInfo(getImagesDefProps());
        LinkedHashMap<String, Object> customDefs = buildCustomTypeDefinitions(type, typeInfo);
        smnodetemplates.add(new NodeTemplate(name, ntnodeTemplates, customDefs, null, null));
    }

    /**
     * Simulate the creation of a NodeTemplate by the SDC TOSCA parser. Populate the properties of the NodeTemplate with
     * the supplied images.
     * 
     * @param images
     *            the value of the images property
     */
    public void addNodeTemplate(Object images) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("images", images);

        String type = "tosca.nodes.custom";
        LinkedHashMap<String, Object> nodeTemplate = new LinkedHashMap<>();
        nodeTemplate.put("type", type);
        nodeTemplate.put("properties", properties);

        String name = "node name";
        LinkedHashMap<String, Object> ntnodeTemplates = buildCustomTypeDefinitions(name, nodeTemplate);
        ntnodeTemplates.put("derived_from", null);
        ntnodeTemplates.put("properties", getImagesDefProps());

        LinkedHashMap<String, Object> typeInfo = buildNodeTemplateTypeInfo(getImagesDefProps());
        LinkedHashMap<String, Object> customDefs = buildCustomTypeDefinitions(type, typeInfo);

        smnodetemplates.add(new NodeTemplate(name, ntnodeTemplates, customDefs, null, null));
    }
}

