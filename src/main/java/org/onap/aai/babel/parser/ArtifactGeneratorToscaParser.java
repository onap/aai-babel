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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.GroupConfiguration;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Widget;
import org.onap.aai.babel.xml.generator.model.WidgetType;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.Property;
import org.onap.sdc.toscaparser.api.elements.Metadata;

/**
 * Wrapper for the sdc-tosca parser.
 *
 */
public class ArtifactGeneratorToscaParser {

    private static Logger log = LogHelper.INSTANCE;

    public static final String PROPERTY_TOSCA_MAPPING_FILE = "tosca.mappings.config";

    public static final String GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND =
            "Cannot generate artifacts. System property %s not configured";

    private static final String GENERATOR_AAI_CONFIGFILE_NOT_FOUND =
            "Cannot generate artifacts. Artifact Generator Configuration file not found at %s";
    private static final String GENERATOR_AAI_PROVIDING_SERVICE_METADATA_MISSING =
            "Cannot generate artifacts. Providing Service Metadata is missing for allotted resource %s";
    private static final String GENERATOR_AAI_PROVIDING_SERVICE_MISSING =
            "Cannot generate artifacts. Providing Service is missing for allotted resource %s";

    // Metadata properties
    private static final String CATEGORY = "category";
    private static final String ALLOTTED_RESOURCE = "Allotted Resource";
    private static final String SUBCATEGORY = "subcategory";
    private static final String TUNNEL_XCONNECT = "Tunnel XConnect";

    private static final String VERSION = "version";

    private ISdcCsarHelper csarHelper;

    /**
     * Constructs using csarHelper
     *
     * @param csarHelper
     *            The csar helper
     */
    public ArtifactGeneratorToscaParser(ISdcCsarHelper csarHelper) {
        this.csarHelper = csarHelper;
    }

    /**
     * Initializes the group filtering and TOSCA to Widget mapping configuration.
     *
     * @param configLocation
     *            the pathname to the JSON mappings file
     * @throws IOException
     *             if the file content could not be read successfully
     */
    public static void initToscaMappingsConfiguration(String configLocation) throws IOException {
        log.debug("Getting TOSCA Mappings Configuration");
        File file = new File(configLocation);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format(GENERATOR_AAI_CONFIGFILE_NOT_FOUND, configLocation));
        }

        GroupConfiguration config;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configLocation))) {
            config = new Gson().fromJson(bufferedReader, GroupConfiguration.class);
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid Mappings Configuration " + configLocation, e);
        }

        if (config == null) {
            throw new IOException("There is no content for the Mappings Configuration " + configLocation);
        }

        WidgetConfigurationUtil.setSupportedInstanceGroups(config.getInstanceGroupTypes());
        WidgetConfigurationUtil.setWidgetTypes(config.getWidgetTypes());
        WidgetConfigurationUtil.setWidgetMappings(config.getWidgetMappings());
    }

    /**
     * Process groups for this service node, according to the defined filter.
     *
     * @param resourceModel
     * @param serviceNodeTemplate
     * @return resources for which XML Models should be generated
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for a member Widget of an instance group
     */
    public List<Resource> processInstanceGroups(Model resourceModel, NodeTemplate serviceNodeTemplate)
            throws XmlArtifactGenerationException {
        List<Resource> resources = new ArrayList<>();
        if (serviceNodeTemplate.getSubMappingToscaTemplate() != null) {
            List<Group> serviceGroups = csarHelper.getGroupsOfOriginOfNodeTemplate(serviceNodeTemplate);
            for (Group group : serviceGroups) {
                if (WidgetConfigurationUtil.isSupportedInstanceGroup(group.getType())) {
                    resources.addAll(processInstanceGroup(resourceModel, group.getMemberNodes(),
                            group.getMetadata().getAllProperties(), group.getProperties()));
                }
            }
        }
        return resources;
    }

    /**
     * Merge a Map of String values with a Map of TOSCA Property Objects to create a combined Map. If there are
     * duplicate keys then the TOSCA Property value takes precedence.
     *
     * @param stringProps
     *            initial Map of String property values (e.g. from the TOSCA YAML metadata section)
     * @param toscaProps
     *            Map of TOSCA Property Type Object values to merge in (or overwrite)
     * @return a Map of the property values converted to String
     */
    public Map<String, String> mergeProperties(Map<String, String> stringProps, Map<String, Property> toscaProps) {
        Map<String, String> props = new HashMap<>(stringProps);
        toscaProps.forEach((key, toscaProp) -> props.put(key,
                toscaProp.getValue() == null ? "" : toscaProp.getValue().toString()));
        return props;
    }

    public Resource createInstanceGroupModel(Map<String, String> properties) {
        Resource groupModel = new Resource(WidgetType.valueOf("INSTANCE_GROUP"), true);
        groupModel.populateModelIdentificationInformation(properties);
        return groupModel;
    }

    /**
     * Add the resource/widget to the specified model.
     *
     * @param model
     * @param relation
     *            resource or widget model to add
     * @throws XmlArtifactGenerationException
     *             if the relation is a widget and there is no configuration defined for the relation's widget type
     */
    public void addRelatedModel(final Model model, final Resource relation) throws XmlArtifactGenerationException {
        if (relation.getModelType() == ModelType.RESOURCE) {
            model.addResource(relation);
        } else {
            model.addWidget(Widget.createWidget(relation.getWidgetType()));
        }
    }

    public boolean hasAllottedResource(Map<String, String> metadata) {
        return ALLOTTED_RESOURCE.equals(metadata.get(CATEGORY));
    }

    public boolean hasSubCategoryTunnelXConnect(Map<String, String> metadata) {
        return TUNNEL_XCONNECT.equals(metadata.get(SUBCATEGORY));
    }

    /**
     * Process TOSCA Group information for VF Modules.
     *
     * @param resources
     * @param model
     * @param serviceNode
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support the widget type of a VF Module
     */
    public void processVfModules(List<Resource> resources, Model resourceModel, NodeTemplate serviceNode)
            throws XmlArtifactGenerationException {
        // Get the customization UUID for each VF node and use it to get its Groups
        String uuid = csarHelper.getNodeTemplateCustomizationUuid(serviceNode);
        List<Group> serviceGroups = csarHelper.getVfModulesByVf(uuid);

        // Process each VF Group
        for (Group serviceGroup : serviceGroups) {
            Model groupModel = Model.getModelFor(serviceGroup.getType());
            if (groupModel.hasWidgetType("VFMODULE")) {
                processVfModule(resources, resourceModel, serviceGroup, serviceNode, (Resource) groupModel);
            }
        }
    }

    /**
     * @param resourceModel
     * @param resourceNodeTemplates
     */
    public void processResourceModels(Model resourceModel, List<NodeTemplate> resourceNodeTemplates) {
        boolean foundProvidingService = false;

        for (NodeTemplate resourceNodeTemplate : resourceNodeTemplates) {
            String nodeTypeName = resourceNodeTemplate.getType();
            Metadata metadata = resourceNodeTemplate.getMetaData();
            String metaDataType = Optional.ofNullable(metadata).map(m -> m.getValue("type")).orElse(nodeTypeName);
            Resource model = Model.getModelFor(nodeTypeName, metaDataType);

            if (metadata != null && hasAllottedResource(metadata.getAllProperties())
                    && model.hasWidgetType("VSERVER")) {
                model = new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), false);
                Map<String, Object> props = new HashMap<>();
                props.put("providingService", true);
                model.setProperties(props);
            }

            foundProvidingService |= processModel(resourceModel, metadata, model, resourceNodeTemplate.getProperties());
        }

        if (resourceModel.hasWidgetType("ALLOTTED_RESOURCE") && !foundProvidingService) {
            final String modelInvariantId = resourceModel.getModelId();
            throw new IllegalArgumentException(String.format(GENERATOR_AAI_PROVIDING_SERVICE_MISSING,
                    modelInvariantId == null ? "<null ID>" : modelInvariantId));
        }
    }

    /**
     * Create an Instance Group Model and populate it with the supplied data.
     *
     * @param resourceModel
     *            the Resource node template Model
     * @param memberNodes
     *            the Resources and Widgets belonging to the Group
     * @param metaProperties
     *            the metadata of the Group
     * @param properties
     *            the properties of the Group
     * @return the Instance Group and Member resource models
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for one of the member Widgets
     */
    private List<Resource> processInstanceGroup(Model resourceModel, ArrayList<NodeTemplate> memberNodes,
            Map<String, String> metaProperties, Map<String, Property> properties)
            throws XmlArtifactGenerationException {
        Resource groupModel = createInstanceGroupModel(mergeProperties(metaProperties, properties));
        resourceModel.addResource(groupModel);
        List<Resource> resources = Stream.of(groupModel).collect(Collectors.toList());

        if (memberNodes != null && !memberNodes.isEmpty()) {
            resources.addAll(generateResourcesAndWidgets(memberNodes, groupModel));
        }

        return resources;
    }

    /**
     * @param memberNodes
     * @param groupModel
     * @return a list of Resources
     * @throws XmlArtifactGenerationException
     *             if a member node template is a widget and there is no configuration defined for that relation's
     *             widget type
     */
    private List<Resource> generateResourcesAndWidgets(final ArrayList<NodeTemplate> memberNodes,
            final Resource groupModel) throws XmlArtifactGenerationException {
        log.debug(String.format("Processing member nodes for Group %s (invariant UUID %s)", //
                groupModel.getModelName(), groupModel.getModelId()));

        List<Resource> resources = new ArrayList<>();

        for (NodeTemplate nodeTemplate : memberNodes) {
            String nodeTypeName = nodeTemplate.getType();
            final String metadataType = nodeTemplate.getMetaData().getValue("type");

            log.debug(String.format("Get model for %s (metadata type %s)", nodeTypeName, metadataType));
            Resource memberModel = Model.getModelFor(nodeTypeName, metadataType);

            if (memberModel != null) {
                memberModel.populateModelIdentificationInformation(nodeTemplate.getMetaData().getAllProperties());

                log.debug(String.format("Generating grouped %s (%s) from TOSCA type %s",
                        memberModel.getClass().getSuperclass().getSimpleName(), memberModel.getClass(), nodeTypeName));

                addRelatedModel(groupModel, memberModel);
                if (memberModel.getModelType() == ModelType.RESOURCE) {
                    resources.add(memberModel);
                }
            }
        }
        return resources;
    }

    /**
     * @param resources
     * @param vfModel
     * @param groupDefinition
     * @param serviceNode
     * @param groupModel
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support the widget type of a VF Module
     */
    private void processVfModule(List<Resource> resources, Model vfModel, Group groupDefinition,
            NodeTemplate serviceNode, Resource groupModel) throws XmlArtifactGenerationException {
        groupModel.populateModelIdentificationInformation(
                mergeProperties(groupDefinition.getMetadata().getAllProperties(), groupDefinition.getProperties()));

        processVfModuleGroup(groupModel, csarHelper.getMembersOfVfModule(serviceNode, groupDefinition));

        vfModel.addResource(groupModel); // Add group (VfModule) to the (VF) model
        // Check if we have already encountered the same VfModule across all the artifacts
        if (!resources.contains(groupModel)) {
            resources.add(groupModel);
        }
    }

    /**
     * @param groupModel
     * @param members
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support the widget type of a member
     */
    private void processVfModuleGroup(Resource groupModel, List<NodeTemplate> members)
            throws XmlArtifactGenerationException {
        if (members != null && !members.isEmpty()) {
            // Get names of the members of the service group
            List<String> memberNames = members.stream().map(NodeTemplate::getName).collect(Collectors.toList());
            groupModel.setMembers(memberNames);
            for (NodeTemplate member : members) {
                processGroupMembers(groupModel, member);
            }
        }
    }

    /**
     * Process the Widget members of a VF Module Group
     *
     * @param group
     *            the group resource model
     * @param member
     *            the group member to process
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support the widget type of the member
     */
    private void processGroupMembers(Resource group, NodeTemplate member) throws XmlArtifactGenerationException {
        Resource resource = Model.getModelFor(member.getType());

        log.debug(member.getType() + " mapped to " + resource);

        if (resource.hasWidgetType("L3_NET")) {
            // An l3-network inside a vf-module is treated as a Widget
            resource.setModelType(ModelType.WIDGET);
        }

        if (resource.getModelType() == ModelType.WIDGET) {
            Widget widget = Widget.createWidget(resource.getWidgetType());
            widget.addKey(member.getName());
            // Add the widget element encountered to the Group model
            group.addWidget(widget);
        }
    }

    /**
     * Create a Map of property name against String property value from the input Map
     *
     * @param inputMap
     *            The input Map
     * @return Map of property name against String property value
     */
    private Map<String, String> populateStringProperties(Map<String, Property> inputMap) {
        return inputMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().getValue() == null ? "" : e.getValue().getValue().toString()));
    }

    /**
     * If the specified resourceNode is a type of Resource, add it to the specified resourceModel. If the Resource type
     * is ProvidingService then return true, otherwise return false.
     *
     * @param resourceModel
     *            parent Resource
     * @param metaData
     *            for populating the Resource IDs
     * @param resourceNode
     *            any Model (will be ignored if not a Resource)
     * @param nodeProperties
     *            the node properties
     * @return whether or not a ProvidingService was processed
     */
    private boolean processModel(Model resourceModel, Metadata metaData, Resource resourceNode,
            Map<String, Property> nodeProperties) {
        boolean foundProvidingService = resourceNode != null
                && (boolean) Optional.ofNullable(resourceNode.getProperties().get("providingService")).orElse(false);

        if (foundProvidingService) {
            processProvidingService(resourceModel, resourceNode, nodeProperties);
        } else if (resourceNode != null && resourceNode.getModelType() == ModelType.RESOURCE
                && !resourceNode.hasWidgetType("L3_NET")) {
            if (metaData != null) {
                resourceNode.populateModelIdentificationInformation(metaData.getAllProperties());
            }
            resourceModel.addResource(resourceNode);
        }
        return foundProvidingService;
    }

    private void processProvidingService(Model resourceModel, Resource resourceNode,
            Map<String, Property> nodeProperties) {
        if (nodeProperties == null || nodeProperties.get("providing_service_uuid") == null
                || nodeProperties.get("providing_service_invariant_uuid") == null) {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_PROVIDING_SERVICE_METADATA_MISSING, resourceModel.getModelId()));
        }
        Map<String, String> properties = populateStringProperties(nodeProperties);
        properties.put(VERSION, "1.0");
        resourceNode.populateModelIdentificationInformation(properties);
        resourceModel.addResource(resourceNode);
    }
}
