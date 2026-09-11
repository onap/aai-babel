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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.onap.sdc.tosca.parser.impl.SdcPropertyNames;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.Property;
import org.onap.sdc.toscaparser.api.SubstitutionMappings;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.onap.sdc.toscaparser.api.elements.StatefulEntityType;

/**
 * Direct tests of the TOSCA parser-based Artifact Generator {@link ArtifactGeneratorToscaParser}., to cover exceptional
 * cases.
 */

public class TestArtifactGeneratorToscaParser {

    private static final String TEST_UUID = "1234";
    private static final String CATEGORY = "category";
    private static final String ALLOTTED_RESOURCE = "Allotted Resource";
    private static final String VSERVER_NODE_TYPE = "org.openecomp.resource.vfc.compute";
    private static final String PNF_NODE_TYPE = "org.openecomp.resource.pnf.example";
    private static final String LINT_NODE_TYPE = "org.openecomp.resource.cp.example";
    private static final String VF_MODULE_GROUP_TYPE = "org.openecomp.groups.VfModule";

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
     * A Providing Service node template with no TOSCA properties at all cannot be processed.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     */
    @Test
    public void testProvidingServiceWithoutPropertiesIsRejected() throws IOException {
        new ArtifactTestUtils().loadWidgetMappings();

        NodeTemplate providingService = mockNodeTemplate("providingService", VSERVER_NODE_TYPE,
                mockMetadata(Collections.singletonMap(CATEGORY, ALLOTTED_RESOURCE)));
        Mockito.when(providingService.getProperties()).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ArtifactGeneratorToscaParser(null).processResourceModels(
                        new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true),
                        Collections.singletonList(providingService)));

        assertThat(exception.getMessage(), containsString("Providing Service Metadata is missing"));
    }

    /**
     * A Providing Service node template must supply both of the providing service UUIDs.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     */
    @Test
    public void testProvidingServiceWithoutInvariantUuidIsRejected() throws IOException {
        new ArtifactTestUtils().loadWidgetMappings();

        NodeTemplate providingService = mockNodeTemplate("providingService", VSERVER_NODE_TYPE,
                mockMetadata(Collections.singletonMap(CATEGORY, ALLOTTED_RESOURCE)));
        LinkedHashMap<String, Property> toscaProperties =
                mockProperties(Collections.singletonMap("providing_service_uuid", TEST_UUID));
        Mockito.when(providingService.getProperties()).thenReturn(toscaProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ArtifactGeneratorToscaParser(null).processResourceModels(
                        new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true),
                        Collections.singletonList(providingService)));

        assertThat(exception.getMessage(), containsString("Providing Service Metadata is missing"));
    }

    /**
     * A Providing Service node template must supply both of the providing service UUIDs.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     */
    @Test
    public void testProvidingServiceWithoutServiceUuidIsRejected() throws IOException {
        new ArtifactTestUtils().loadWidgetMappings();

        NodeTemplate providingService = mockNodeTemplate("providingService", VSERVER_NODE_TYPE,
                mockMetadata(Collections.singletonMap(CATEGORY, ALLOTTED_RESOURCE)));
        LinkedHashMap<String, Property> toscaProperties =
                mockProperties(Collections.singletonMap("providing_service_invariant_uuid", "invariant-1234"));
        Mockito.when(providingService.getProperties()).thenReturn(toscaProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ArtifactGeneratorToscaParser(null).processResourceModels(
                        new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true),
                        Collections.singletonList(providingService)));

        assertThat(exception.getMessage(), containsString("Providing Service Metadata is missing"));
    }

    /**
     * Process a valid Providing Service, including a property that has no value.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the Providing Service is not found
     */
    @Test
    public void testProvidingServiceIsAddedToAllottedResource() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("providing_service_uuid", TEST_UUID);
        properties.put("providing_service_invariant_uuid", "invariant-1234");
        properties.put("providing_service_name", null);

        NodeTemplate providingService = mockNodeTemplate("providingService", VSERVER_NODE_TYPE,
                mockMetadata(Collections.singletonMap(CATEGORY, ALLOTTED_RESOURCE)));
        LinkedHashMap<String, Property> toscaProperties = mockProperties(properties);
        Mockito.when(providingService.getProperties()).thenReturn(toscaProperties);

        Resource allottedResource = new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true);
        new ArtifactGeneratorToscaParser(null).processResourceModels(allottedResource,
                Collections.singletonList(providingService));

        assertThat(allottedResource.getResources(), hasSize(1));
        Resource model = allottedResource.getResources().iterator().next();
        assertThat(model.getModelNameVersionId(), is(equalTo(TEST_UUID)));
        assertThat(model.getModelId(), is(equalTo("invariant-1234")));
        assertThat(model.getModelVersion(), is(equalTo("1.0")));
        assertThat(model.getModelName(), is(equalTo("")));
    }

    /**
     * An Allotted Resource category only implies a Providing Service for a vserver.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the resource cannot be added
     */
    @Test
    public void testAllottedResourceCategoryIsIgnoredForNonVserver() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        NodeTemplate pnf = mockNodeTemplate("pnf", PNF_NODE_TYPE,
                mockMetadata(Collections.singletonMap(CATEGORY, ALLOTTED_RESOURCE)));

        Resource vfModel = new Resource(WidgetType.valueOf("VF"), true);
        new ArtifactGeneratorToscaParser(null).processResourceModels(vfModel, Collections.singletonList(pnf));

        assertThat(vfModel.getResources(), hasSize(1));
        assertThat(vfModel.getResources().iterator().next().getWidgetType(), is(equalTo(WidgetType.valueOf("PNF"))));
    }

    /**
     * A node template without any metadata is still added to the parent model, but no identification information can be
     * populated.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the resource cannot be added
     */
    @Test
    public void testResourceWithoutMetadataIsAdded() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        NodeTemplate pnf = mockNodeTemplate("pnf", PNF_NODE_TYPE, null);

        Resource vfModel = new Resource(WidgetType.valueOf("VF"), true);
        new ArtifactGeneratorToscaParser(null).processResourceModels(vfModel, Collections.singletonList(pnf));

        assertThat(vfModel.getResources(), hasSize(1));
        assertThat(vfModel.getResources().iterator().next().getModelId(), is(nullValue()));
    }

    /**
     * Process an Instance Group that has no member nodes at all.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget
     */
    @Test
    public void testInstanceGroupWithNullMemberNodes() throws IOException, XmlArtifactGenerationException {
        assertThat(processInstanceGroupMembers(null), hasSize(1));
    }

    /**
     * Process an Instance Group that has an empty list of member nodes.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget
     */
    @Test
    public void testInstanceGroupWithEmptyMemberNodes() throws IOException, XmlArtifactGenerationException {
        assertThat(processInstanceGroupMembers(new ArrayList<>()), hasSize(1));
    }

    /**
     * An Instance Group member with a TOSCA type that has no Widget mapping is ignored.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget
     */
    @Test
    public void testInstanceGroupMemberWithUnmappedTypeIsSkipped() throws IOException, XmlArtifactGenerationException {
        NodeTemplate member = mockNodeTemplate("member", "org.openecomp.unmapped.type", mockMetadata(new HashMap<>()));

        List<Resource> resources = processInstanceGroupMembers(new ArrayList<>(Arrays.asList(member)));

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getResources(), is(empty()));
        assertThat(resources.get(0).getWidgets(), is(empty()));
    }

    /**
     * An Instance Group member that maps to a Widget is added to the group but is not itself a Resource.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget
     */
    @Test
    public void testInstanceGroupWidgetMemberIsNotReturnedAsResource()
            throws IOException, XmlArtifactGenerationException {
        NodeTemplate member = mockNodeTemplate("member", LINT_NODE_TYPE, mockMetadata(new HashMap<>()));

        List<Resource> resources = processInstanceGroupMembers(new ArrayList<>(Arrays.asList(member)));

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getResources(), is(empty()));
        assertThat(resources.get(0).getWidgets(), hasSize(1));
    }

    /**
     * Only service level groups of type VfModule are processed.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testNonVfModuleServiceGroupIsIgnored() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Group group = mockGroup("vfnode0", VF_MODULE_GROUP_TYPE, "org.openecomp.groups.ResourceInstanceGroup",
                mockMetadata(new HashMap<>()));

        List<Resource> resources = processVfModules(mockCsarHelper(group), mockNodeTemplate("vfnode", null, null));

        assertThat(resources, is(empty()));
    }

    /**
     * A service level group is only processed if its TOSCA type maps to the VF Module Widget type. The group is
     * selected by its type definition, but the model is created from the node template type, and these can differ.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testServiceGroupWithoutVfModuleWidgetTypeIsSkipped() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Group group = mockGroup("vfnode0", PNF_NODE_TYPE, VF_MODULE_GROUP_TYPE, mockMetadata(new HashMap<>()));

        List<Resource> resources = processVfModules(mockCsarHelper(group), mockNodeTemplate("vfnode", null, null));

        assertThat(resources, is(empty()));
    }

    /**
     * Process a VF Module group that has neither metadata nor substitution mappings.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testVfModuleWithoutMetadataIsProcessed() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Group group = mockGroup("vfnode0", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, null);
        LinkedHashMap<String, Property> toscaProperties =
                mockProperties(Collections.singletonMap("vfModuleModelUUID", TEST_UUID));
        Mockito.when(group.getProperties()).thenReturn(toscaProperties);

        List<Resource> resources = processVfModules(mockCsarHelper(group), mockNodeTemplate("vfnode", null, null));

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getModelNameVersionId(), is(equalTo(TEST_UUID)));
    }

    /**
     * The same VF Module may be encountered more than once across the service level groups, but is only reported once.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testDuplicateVfModuleIsReportedOnlyOnce() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Metadata metadata = mockMetadata(Collections.singletonMap("vfModuleModelUUID", TEST_UUID));
        Group group = mockGroup("vfnode0", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, metadata);
        Group duplicate = mockGroup("vfnode1", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, metadata);

        List<Resource> resources =
                processVfModules(mockCsarHelper(group, duplicate), mockNodeTemplate("vfnode", null, null));

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getModelNameVersionId(), is(equalTo(TEST_UUID)));
    }

    /**
     * A substitution mapping group without metadata cannot match the VF Module, so the VF Module has no members.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testVfModuleWithNoMatchingGroupHasNoMembers() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Group unmatchedGroup = mockGroup("othergroup", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, null);
        NodeTemplate member = mockNodeTemplate("member", LINT_NODE_TYPE, mockMetadata(new HashMap<>()));

        List<Resource> resources = processVfModuleMembers(unmatchedGroup, member);

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getWidgets(), is(empty()));
    }

    /**
     * A VF Module member that maps to a Resource (rather than a Widget) is not added to the VF Module group.
     *
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    @Test
    public void testVfModuleResourceMemberIsNotAddedAsWidget() throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        Metadata matchingMetadata =
                mockMetadata(Collections.singletonMap(SdcPropertyNames.PROPERTY_NAME_VFMODULEMODELINVARIANTUUID,
                        "invariant-1234"));
        Group matchedGroup = mockGroup("membergroup", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, matchingMetadata);
        Mockito.when(matchedGroup.getMembers()).thenReturn(new ArrayList<>(Arrays.asList("member")));
        NodeTemplate member = mockNodeTemplate("member", PNF_NODE_TYPE, mockMetadata(new HashMap<>()));

        List<Resource> resources = processVfModuleMembers(matchedGroup, member);

        assertThat(resources, hasSize(1));
        assertThat(resources.get(0).getWidgets(), is(empty()));
    }

    /**
     * Process a single Instance Group with the supplied member node templates.
     *
     * @param memberNodes
     *            the member node templates of the group (may be null)
     * @return the Instance Group and member resource models
     * @throws IOException
     *             if the widget mappings cannot be loaded
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget
     */
    private List<Resource> processInstanceGroupMembers(ArrayList<NodeTemplate> memberNodes)
            throws IOException, XmlArtifactGenerationException {
        new ArtifactTestUtils().loadWidgetMappings();

        final String instanceGroupType = "org.openecomp.groups.ResourceInstanceGroup";
        WidgetConfigurationUtil.setSupportedInstanceGroups(Collections.singletonList(instanceGroupType));

        Group group = mockGroup("group", instanceGroupType, instanceGroupType,
                mockMetadata(Collections.singletonMap("UUID", TEST_UUID)));
        Mockito.when(group.getMemberNodes()).thenReturn(memberNodes);

        SubstitutionMappings sm = Mockito.mock(SubstitutionMappings.class);
        Mockito.when(sm.getGroups()).thenReturn(new ArrayList<>(Arrays.asList(group)));

        NodeTemplate serviceNodeTemplate = mockNodeTemplate("service", null, null);
        Mockito.when(serviceNodeTemplate.getSubMappingToscaTemplate()).thenReturn(sm);

        return new ArtifactGeneratorToscaParser(Mockito.mock(ISdcCsarHelper.class))
                .processInstanceGroups(new Resource(WidgetType.valueOf("INSTANCE_GROUP"), true), serviceNodeTemplate);
    }

    /**
     * Process a VF Module whose VF node template has substitution mappings containing the supplied group and member.
     *
     * @param subMappingGroup
     *            the single group of the substitution mappings
     * @param member
     *            the single node template of the substitution mappings
     * @return the VF Module resource models
     * @throws XmlArtifactGenerationException
     *             if the VF Module cannot be processed
     */
    private List<Resource> processVfModuleMembers(Group subMappingGroup, NodeTemplate member)
            throws XmlArtifactGenerationException {
        Metadata metadata = mockMetadata(
                Map.of("vfModuleModelUUID", TEST_UUID, SdcPropertyNames.PROPERTY_NAME_VFMODULEMODELINVARIANTUUID,
                        "invariant-1234"));
        Group vfModuleGroup = mockGroup("vfnode0", VF_MODULE_GROUP_TYPE, VF_MODULE_GROUP_TYPE, metadata);

        SubstitutionMappings sm = Mockito.mock(SubstitutionMappings.class);
        Mockito.when(sm.getGroups()).thenReturn(new ArrayList<>(Arrays.asList(subMappingGroup)));
        Mockito.when(sm.getNodeTemplates()).thenReturn(new ArrayList<>(Arrays.asList(member)));

        NodeTemplate serviceVfNode = mockNodeTemplate("vfnode", null, null);
        Mockito.when(serviceVfNode.getSubMappingToscaTemplate()).thenReturn(sm);

        return processVfModules(mockCsarHelper(vfModuleGroup), serviceVfNode);
    }

    private List<Resource> processVfModules(ISdcCsarHelper csarHelper, NodeTemplate serviceVfNode)
            throws XmlArtifactGenerationException {
        List<Resource> resources = new ArrayList<>();
        new ArtifactGeneratorToscaParser(csarHelper).processVfModules(resources,
                new Resource(WidgetType.valueOf("VF"), true), serviceVfNode);
        return resources;
    }

    @SuppressWarnings("deprecation")
    private ISdcCsarHelper mockCsarHelper(Group... serviceLevelGroups) {
        ISdcCsarHelper csarHelper = Mockito.mock(ISdcCsarHelper.class);
        Mockito.when(csarHelper.getGroupsOfTopologyTemplate())
                .thenReturn(new ArrayList<>(Arrays.asList(serviceLevelGroups)));
        return csarHelper;
    }

    private Group mockGroup(String name, String type, String typeDefinitionType, Metadata metadata) {
        StatefulEntityType typeDefinition = Mockito.mock(StatefulEntityType.class);
        Mockito.when(typeDefinition.getType()).thenReturn(typeDefinitionType);

        Group group = Mockito.mock(Group.class);
        Mockito.when(group.getName()).thenReturn(name);
        Mockito.when(group.getType()).thenReturn(type);
        Mockito.when(group.getTypeDefinition()).thenReturn(typeDefinition);
        Mockito.when(group.getMetadata()).thenReturn(metadata);
        Mockito.when(group.getProperties()).thenReturn(new LinkedHashMap<>());
        return group;
    }

    private NodeTemplate mockNodeTemplate(String name, String type, Metadata metadata) {
        NodeTemplate nodeTemplate = Mockito.mock(NodeTemplate.class);
        Mockito.when(nodeTemplate.getName()).thenReturn(name);
        Mockito.when(nodeTemplate.getType()).thenReturn(type);
        Mockito.when(nodeTemplate.getMetaData()).thenReturn(metadata);
        return nodeTemplate;
    }

    private Metadata mockMetadata(Map<String, String> properties) {
        Metadata metadata = Mockito.mock(Metadata.class);
        Mockito.when(metadata.getAllProperties()).thenReturn(properties);
        properties.forEach((key, value) -> Mockito.when(metadata.getValue(key)).thenReturn(value));
        return metadata;
    }

    private LinkedHashMap<String, Property> mockProperties(Map<String, String> properties) {
        LinkedHashMap<String, Property> toscaProperties = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            Property property = Mockito.mock(Property.class);
            Mockito.when(property.getValue()).thenReturn(value);
            toscaProperties.put(key, property);
        });
        return toscaProperties;
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
