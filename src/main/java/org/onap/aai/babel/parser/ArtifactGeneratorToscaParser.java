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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.AllotedResource;
import org.onap.aai.babel.xml.generator.model.InstanceGroup;
import org.onap.aai.babel.xml.generator.model.L3NetworkWidget;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.ProvidingService;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.VfModule;
import org.onap.aai.babel.xml.generator.model.Widget;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.Property;
import org.onap.sdc.toscaparser.api.elements.Metadata;

public class ArtifactGeneratorToscaParser {

    private static Logger log = LogHelper.INSTANCE;

    public static final String PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE = "artifactgenerator.config";
    public static final String PROPERTY_GROUP_FILTERS_CONFIG_FILE = "groupfilter.config";

    private static final String GENERATOR_AAI_CONFIGFILE_NOT_FOUND =
            "Cannot generate artifacts. Artifact Generator Configuration file not found at %s";
    private static final String GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND =
            "Cannot generate artifacts. System property %s not configured";
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
     *        The csar helper
     */
    public ArtifactGeneratorToscaParser(ISdcCsarHelper csarHelper) {
        this.csarHelper = csarHelper;
    }

    /**
     * Returns the artifact description
     *
     * @param model
     *        the artifact model
     * @return the artifact model's description
     */
    public static String getArtifactDescription(Model model) {
        String artifactDesc = model.getModelDescription();
        if (model.getModelType().equals(ModelType.SERVICE)) {
            artifactDesc = "AAI Service Model";
        } else if (model.getModelType().equals(ModelType.RESOURCE)) {
            artifactDesc = "AAI Resource Model";
        }
        return artifactDesc;
    }

    /**
     * Initialises the widget configuration.
     *
     * @throws IOException
     */
    public static void initWidgetConfiguration() throws IOException {
        log.debug("Getting Widget Configuration");
        String configLocation = System.getProperty(PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE);
        if (configLocation != null) {
            File file = new File(configLocation);
            if (file.exists()) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(file));
                WidgetConfigurationUtil.setConfig(properties);
            } else {
                throw new IllegalArgumentException(String.format(GENERATOR_AAI_CONFIGFILE_NOT_FOUND, configLocation));
            }
        } else {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND, PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE));
        }
    }

    /**
     * Initialises the group filter configuration.
     *
     * @throws IOException
     */
    public static void initGroupFilterConfiguration() throws IOException {
        log.debug("Getting Filter Tyoes Configuration");
        String configLocation = System.getProperty(PROPERTY_GROUP_FILTERS_CONFIG_FILE);
        if (configLocation != null) {
            File file = new File(configLocation);
            if (file.exists()) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(file));
                WidgetConfigurationUtil.setFilterConfig(properties);
            } else {
                throw new IllegalArgumentException(String.format(GENERATOR_AAI_CONFIGFILE_NOT_FOUND, configLocation));
            }
        } else {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND, PROPERTY_GROUP_FILTERS_CONFIG_FILE));
        }
    }

    /**
     * Process groups for this service node, according to the defined filter.
     *
     * @param resourceModel
     * @param serviceNodeTemplate
     * @return resources for which XML Models should be generated
     */
    public List<Resource> processInstanceGroups(Model resourceModel, NodeTemplate serviceNodeTemplate) {
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
     *        initial Map of String property values (e.g. from the TOSCA YAML metadata section)
     * @param toscaProps
     *        Map of TOSCA Property Type Object values to merge in (or overwrite)
     * @return a Map of the property values converted to String
     */
    public Map<String, String> mergeProperties(Map<String, String> stringProps, Map<String, Property> toscaProps) {
        Map<String, String> props = new HashMap<>(stringProps);
        toscaProps.forEach((key, toscaProp) -> props.put(key,
                toscaProp.getValue() == null ? "" : toscaProp.getValue().toString()));
        return props;
    }

    public Resource createInstanceGroupModel(Map<String, String> properties) {
        Resource groupModel = new InstanceGroup();
        groupModel.populateModelIdentificationInformation(properties);
        return groupModel;
    }

    /**
     * @param model
     * @param relation
     */
    public void addRelatedModel(final Model model, final Model relation) {
        if (relation instanceof Resource) {
            model.addResource((Resource) relation);
        } else {
            model.addWidget((Widget) relation);
        }
    }

    public String normaliseNodeTypeName(NodeTemplate nodeType) {
        String nodeTypeName = nodeType.getType();
        Metadata metadata = nodeType.getMetaData();
        if (metadata != null && hasAllottedResource(metadata.getAllProperties())) {
            if (nodeType.getType().contains("org.openecomp.resource.vf.")) {
                nodeTypeName = "org.openecomp.resource.vf.allottedResource";
            }
            if (nodeType.getType().contains("org.openecomp.resource.vfc.")) {
                nodeTypeName = "org.openecomp.resource.vfc.AllottedResource";
            }
        }
        return nodeTypeName;
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
     */
    public void processVfModules(List<Resource> resources, Model resourceModel, NodeTemplate serviceNode) {
        // Get the customisation UUID for each VF node and use it to get its Groups
        String uuid = csarHelper.getNodeTemplateCustomizationUuid(serviceNode);
        List<Group> serviceGroups = csarHelper.getVfModulesByVf(uuid);

        // Process each VF Group
        for (Group serviceGroup : serviceGroups) {
            Model groupModel = Model.getModelFor(serviceGroup.getType());
            if (groupModel instanceof VfModule) {
                processVfModule(resources, resourceModel, serviceGroup, serviceNode, (VfModule) groupModel);
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
            String nodeTypeName = normaliseNodeTypeName(resourceNodeTemplate);
            Metadata metaData = resourceNodeTemplate.getMetaData();
            String metaDataType = Optional.ofNullable(metaData).map(m -> m.getValue("type")).orElse(nodeTypeName);
            Model resourceNode = Model.getModelFor(nodeTypeName, metaDataType);
            foundProvidingService |=
                    processModel(resourceModel, metaData, resourceNode, resourceNodeTemplate.getProperties());
        }

        if (resourceModel instanceof AllotedResource && !foundProvidingService) {
            final String modelInvariantId = resourceModel.getModelId();
            throw new IllegalArgumentException(String.format(GENERATOR_AAI_PROVIDING_SERVICE_MISSING,
                    modelInvariantId == null ? "<null ID>" : modelInvariantId));
        }
    }

    /**
     * Create an Instance Group Model and populate it with the supplied data.
     *
     * @param resourceModel
     *        the Resource node template Model
     * @param memberNodes
     *        the Resources and Widgets belonging to the Group
     * @param metaProperties
     *        the metadata of the Group
     * @param properties
     *        the properties of the Group
     * @return the Instance Group and Member resource models
     */
    private List<Resource> processInstanceGroup(Model resourceModel, ArrayList<NodeTemplate> memberNodes,
            Map<String, String> metaProperties, Map<String, Property> properties) {
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
     * @return
     */
    private List<Resource> generateResourcesAndWidgets(final ArrayList<NodeTemplate> memberNodes,
            final Resource groupModel) {
        List<Resource> resources = new ArrayList<>();
        for (NodeTemplate nodeTemplate : memberNodes) {
            String nodeTypeName = normaliseNodeTypeName(nodeTemplate);
            Model memberModel = Model.getModelFor(nodeTypeName, nodeTemplate.getMetaData().getValue("type"));
            memberModel.populateModelIdentificationInformation(nodeTemplate.getMetaData().getAllProperties());

            log.debug(String.format("Generating grouped %s (%s) from TOSCA type %s",
                    memberModel.getClass().getSuperclass().getSimpleName(), memberModel.getClass(), nodeTypeName));

            addRelatedModel(groupModel, memberModel);
            if (memberModel instanceof Resource) {
                resources.add((Resource) memberModel);
            }
        }
        return resources;
    }

    private void processVfModule(List<Resource> resources, Model vfModel, Group groupDefinition,
            NodeTemplate serviceNode, VfModule groupModel) {
        groupModel.populateModelIdentificationInformation(
                mergeProperties(groupDefinition.getMetadata().getAllProperties(), groupDefinition.getProperties()));

        processVfModuleGroup(groupModel, csarHelper.getMembersOfVfModule(serviceNode, groupDefinition));

        vfModel.addResource(groupModel); // Add group (VfModule) to the (VF) model
        // Check if we have already encountered the same VfModule across all the artifacts
        if (!resources.contains(groupModel)) {
            resources.add(groupModel);
        }
    }

    private void processVfModuleGroup(VfModule groupModel, List<NodeTemplate> members) {
        if (members != null && !members.isEmpty()) {
            // Get names of the members of the service group
            List<String> memberNames = members.stream().map(NodeTemplate::getName).collect(Collectors.toList());
            groupModel.setMembers(memberNames);
            for (NodeTemplate member : members) {
                processGroupMembers(groupModel, member);
            }
        }
    }

    private void processGroupMembers(Model group, NodeTemplate member) {
        Model resourceNode;
        // L3-network inside vf-module to be generated as Widget a special handling.
        if (member.getType().contains("org.openecomp.resource.vl")) {
            resourceNode = new L3NetworkWidget();
        } else {
            resourceNode = Model.getModelFor(member.getType());
        }
        if (resourceNode != null && !(resourceNode instanceof Resource)) {
            Widget widget = (Widget) resourceNode;
            widget.addKey(member.getName());
            // Add the widget element encountered to the Group model
            group.addWidget(widget);
        }
    }

    /**
     * Create a Map of property name against String property value from the input Map
     *
     * @param inputMap
     *        The input Map
     * @return Map of property name against String property value
     */
    private Map<String, String> populateStringProperties(Map<String, Property> inputMap) {
        return inputMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().getValue() == null ? "" : e.getValue().getValue().toString()));
    }

    /**
     * If the specified resourceNode is a type of Resource, add it to the specified resourceModel. If the Resource type
     * is ProvidingService return true, otherwise return false.
     *
     * @param resourceModel
     *        parent Resource
     * @param resourceNodeTemplate
     * @param metaData
     *        metadata for populating the Resource IDs
     * @param resourceNode
     *        any Model (will be ignored if not a Resource)
     * @return whether or not a ProvidingService was prcoessed
     */
    private boolean processModel(Model resourceModel, Metadata metaData, Model resourceNode,
            Map<String, Property> nodeProperties) {
        boolean foundProvidingService = false;
        if (resourceNode instanceof ProvidingService) {
            foundProvidingService = true;
            processProvidingService(resourceModel, resourceNode, nodeProperties);
        } else if (resourceNode instanceof Resource && !(resourceNode.getWidgetType().equals(Widget.Type.L3_NET))) {
            if (metaData != null) {
                resourceNode.populateModelIdentificationInformation(metaData.getAllProperties());
            }
            resourceModel.addResource((Resource) resourceNode);
        }
        return foundProvidingService;
    }

    private void processProvidingService(Model resourceModel, Model resourceNode,
            Map<String, Property> nodeProperties) {
        if (nodeProperties == null || nodeProperties.get("providing_service_uuid") == null
                || nodeProperties.get("providing_service_invariant_uuid") == null) {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_PROVIDING_SERVICE_METADATA_MISSING, resourceModel.getModelId()));
        }
        Map<String, String> properties = populateStringProperties(nodeProperties);
        properties.put(VERSION, "1.0");
        resourceNode.populateModelIdentificationInformation(properties);
        resourceModel.addResource((Resource) resourceNode);
    }
}
