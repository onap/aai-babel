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

package org.onap.aai.babel.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.util.Resources;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.data.WidgetMapping;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.babel.xml.generator.model.WidgetType;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.SubstitutionMappings;

/**
 * Direct tests of the TOSCA parser-based Artifact Generator {@link ArtifactGeneratorToscaParser}., to cover exceptional
 * cases.
 */

public class TestArtifactGeneratorToscaParser {

    private static final String TEST_UUID = "1234";

    /**
     * Initialize the Generator with an invalid mappings file path.
     * 
     * @throws IOException
     *             if the file content could not be read successfully
     */
    @Test
    public void testMissingMappingsFile() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            ArtifactGeneratorToscaParser.initToscaMappingsConfiguration("non-existent.file");
        });
    }

    /**
     * Initialize the Generator with no Widget Mappings content.
     * 
     * @throws IOException
     *             if the file content could not be read successfully
     */
    @Test
    public void testMissingMappingsContent() throws IOException {
        assertThrows(IOException.class, () -> {
            String emptyJson = new ArtifactTestUtils().getResourcePath(Resources.EMPTY_TOSCA_MAPPING_CONFIG);
            ArtifactGeneratorToscaParser.initToscaMappingsConfiguration(emptyJson);
        });
    }

    /**
     * Initialize the Generator with invalid Widget Mappings content.
     * 
     * @throws IOException
     *             if the file content could not be read successfully
     */
    @Test
    public void testInvalidMappingsContent() throws IOException {
        assertThrows(IOException.class, () -> {
            String invalidJson = new ArtifactTestUtils().getResourcePath(Resources.INVALID_TOSCA_MAPPING_CONFIG);
            ArtifactGeneratorToscaParser.initToscaMappingsConfiguration(invalidJson);
        });
    }

    /**
     * Process an Allotted Resource that does not have a Providing Service.
     *
     * @throws XmlArtifactGenerationException
     *             because the ALLOTTED_RESOURCE lacks a Providing Service
     */
    @Test
    public void testMissingProvidingService() throws XmlArtifactGenerationException {
        assertThrows(XmlArtifactGenerationException.class, () -> {
            List<NodeTemplate> nodeTemplateList = Collections.singletonList(buildNodeTemplate("name", "BlockStorage"));
            new ArtifactGeneratorToscaParser(null)
                    .processResourceModels(new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true), nodeTemplateList);
        });
    }

    /**
     * Add a CR (a type of Resource which is not a Providing Service) to a Resource Model.
     *
     * @throws XmlArtifactGenerationException
     *             because the ALLOTTED_RESOURCE lacks a Providing Service
     * @throws IOException
     *             if the test mappings cannot be loaded
     */
    @Test
    public void testAddResourceNotProvidingService() throws XmlArtifactGenerationException, IOException {
        assertThrows(XmlArtifactGenerationException.class, () -> {
            new ArtifactTestUtils().loadWidgetMappings();
            List<NodeTemplate> nodeTemplateList = Collections.singletonList(buildNodeTemplate("testCR", "CR"));

            // Create any Resource to which the CR can be added
            final Resource dummyResource = new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true);
            new ArtifactGeneratorToscaParser(null).processResourceModels(dummyResource, nodeTemplateList);
        });
    }

    /**
     * Initialize the Artifact Generator Widget Mapping config with incomplete data (no type).
     * 
     * @throws IOException
     *             if a WidgetMapping is invalid
     */
    @Test
    public void testToscaMappingWithoutType() throws IOException {
        assertThrows(IOException.class, () -> {
            WidgetMapping invalidMapping = new WidgetMapping();
            invalidMapping.setType(null);
            WidgetConfigurationUtil.setWidgetMappings(Collections.singletonList(invalidMapping));
        });
    }

    /**
     * Initialize the Artifact Generator Widget Mapping config with invalid data (type value).
     * 
     * @throws IOException
     *             if a WidgetMapping is invalid
     */
    @Test
    public void testToscaMappingWithInvalidType() throws IOException {
        assertThrows(IOException.class, () -> {
            WidgetMapping invalidMapping = new WidgetMapping();
            invalidMapping.setType("invalid");
            WidgetConfigurationUtil.setWidgetMappings(Collections.singletonList(invalidMapping));
        });
    }

    /**
     * Initialize the Artifact Generator Widget Mapping config with incomplete data (no widget name).
     * 
     * @throws IOException
     *             if a WidgetMapping is invalid
     */
    @Test
    public void testToscaMappingWithoutWidget() throws IOException {
        assertThrows(IOException.class, () -> {
            WidgetMapping invalidMapping = new WidgetMapping();
            invalidMapping.setWidget(null);
            WidgetConfigurationUtil.setWidgetMappings(Collections.singletonList(invalidMapping));
        });
    }

    /**
     * Create a Resource with a Widget model type and add this to a Service. Note that there are no test CSAR files
     * which require this functionality, but the code path exists to support it.
     *
     * @throws IOException
     *             if the widget mappings are not loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for the test resource's widget type
     */
    @Test
    public void testAddWidgetToService() throws IOException, XmlArtifactGenerationException {
        assertDoesNotThrow(() -> {
            ArtifactTestUtils testUtils = new ArtifactTestUtils();
            testUtils.loadWidgetMappings();

            Model serviceModel = new Service();
            Resource resourceModel = new Resource(WidgetType.valueOf("VF"), false);
            resourceModel.setModelType(ModelType.WIDGET);

            ISdcCsarHelper helper = Mockito.mock(ISdcCsarHelper.class);
            ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(helper);
            parser.addRelatedModel(serviceModel, resourceModel);
        });
    }

    /**
     * Process a dummy Group object for a Service Resource.
     * 
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget of an instance group
     * @throws IOException
     *             if the widget mappings cannot be loaded
     */
    @Test
    public void testInstanceGroups() throws XmlArtifactGenerationException, IOException {
        new ArtifactTestUtils().loadWidgetMappings();

        final String instanceGroupType = "org.openecomp.groups.ResourceInstanceGroup";
        WidgetConfigurationUtil.setSupportedInstanceGroups(Collections.singletonList(instanceGroupType));

        SubstitutionMappings sm = Mockito.mock(SubstitutionMappings.class);

        List<Group> groups = Arrays.asList(buildGroup("group", instanceGroupType));
        Mockito.when(sm.getGroups()).thenReturn(new ArrayList<Group>(groups));

        NodeTemplate serviceNodeTemplate =
                buildNodeTemplate("service", "org.openecomp.resource.cr.a-collection-resource");
        serviceNodeTemplate.setSubMappingToscaTemplate(sm);

        Resource groupResource = new Resource(WidgetType.valueOf("INSTANCE_GROUP"), true);
        List<Resource> resources = new ArtifactGeneratorToscaParser(Mockito.mock(ISdcCsarHelper.class))
                .processInstanceGroups(groupResource, serviceNodeTemplate);

        assertThat(resources.size(), is(1));
        Resource resource = resources.get(0);
        assertThat(resource.getModelNameVersionId(), is(equalTo(TEST_UUID)));
    }

    /**
     * Create a NodeTemplate for unit testing purposes. In production code this object would only be created by the
     * sdc-tosca parser.
     *
     * @param name
     *            name of the NodeTemplate
     * @param type
     *            type of the NodeTemplate
     * @return a new NodeTemplate object
     */
    private NodeTemplate buildNodeTemplate(String name, String type) {
        LinkedHashMap<String, Object> nodeTemplateMap = new LinkedHashMap<>();
        nodeTemplateMap.put(name, buildMap("type", type));
        nodeTemplateMap.put(type, buildNodeTemplateCustomDefs());
        return new NodeTemplate(name, nodeTemplateMap, nodeTemplateMap, null, null);
    }

    private LinkedHashMap<String, Object> buildNodeTemplateCustomDefs() {
        LinkedHashMap<String, Object> customDefs = buildCustomDefs();
        customDefs.put("attributes", null);
        customDefs.put("requirements", null);
        customDefs.put("capabilities", null);
        customDefs.put("artifacts", null);
        return customDefs;
    }

    private Group buildGroup(String name, String type) {
        LinkedHashMap<String, Object> template = new LinkedHashMap<>();
        template.put("type", type);
        template.put("metadata", new LinkedHashMap<>());
        template.put("properties", buildMap("UUID", TEST_UUID));
        LinkedHashMap<String, Object> customDefMap = buildMap(name, template);
        customDefMap.put(type, buildGroupCustomDefs());
        return new Group(name, template, null, customDefMap);
    }

    private LinkedHashMap<String, Object> buildGroupCustomDefs() {
        LinkedHashMap<String, Object> customDefs = buildCustomDefs();
        customDefs.put("members", null);
        return customDefs;
    }

    private LinkedHashMap<String, Object> buildCustomDefs() {
        LinkedHashMap<String, Object> customDefs = new LinkedHashMap<>();
        customDefs.put("derived_from", null);
        customDefs.put("metadata", null);
        customDefs.put("version", null);
        customDefs.put("description", null);
        customDefs.put("interfaces", null);
        customDefs.put("properties", buildMap("UUID", buildMap("type", "java.lang.String")));
        return customDefs;
    }

    private LinkedHashMap<String, Object> buildMap(String key, Object value) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }
}
