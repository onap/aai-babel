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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.AllotedResource;
import org.onap.aai.babel.xml.generator.model.InstanceGroup;
import org.onap.aai.babel.xml.generator.model.L3NetworkWidget;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.ProvidingService;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.babel.xml.generator.model.TunnelXconnectWidget;
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
     * @param csarHelper The csar helper
     */
    public ArtifactGeneratorToscaParser(ISdcCsarHelper csarHelper) {
        this.csarHelper = csarHelper;
    }

    /**
     * Returns the artifact description
     *
     * @param model the artifact model
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
     * Process the service TOSCA.
     *
     * @param service model of the service artifact
     * @param idTypeStore ID->Type mapping
     * @param nodeTemplates a list of service nodes
     *
     */
    public void processServiceTosca(Service service, Map<String, String> idTypeStore,
            List<NodeTemplate> nodeTemplates) {
        log.debug("Processing (TOSCA) Service object");

        for (NodeTemplate nodeTemplate : nodeTemplates) {
            if (nodeTemplate.getMetaData() != null) {
                addNodeToService(idTypeStore, service, nodeTemplate);
            } else {
                log.warn(ApplicationMsgs.MISSING_SERVICE_METADATA, nodeTemplate.getName());
            }
        }
    }

    /**
     * Generates a Resource List using input Service Node Templates.
     *
     * @param serviceNodes input Service Node Templates
     * @param idTypeStore ID->Type mapping
     *
     * @return the processed resource models
     */
    public List<Resource> processResourceToscas(List<NodeTemplate> serviceNodes, Map<String, String> idTypeStore) {
        List<Resource> resources = new LinkedList<>();
        for (NodeTemplate serviceNode : serviceNodes) {
            if (serviceNode.getMetaData() != null) {
                resources.addAll(processResourceTosca(idTypeStore, serviceNode,
                        csarHelper.getNodeTemplateChildren(serviceNode)));
            } else {
                log.warn(ApplicationMsgs.MISSING_SERVICE_METADATA, serviceNode.getName());
            }
        }
        return resources;
    }

    /**
     * @param idTypeStore ID->Type mapping
     * @param serviceNode
     * @param resourceNodes
     * @return the processed resource models
     */
    private List<Resource> processResourceTosca(Map<String, String> idTypeStore, NodeTemplate serviceNode,
            List<NodeTemplate> resourceNodes) {
        List<Resource> resources = new LinkedList<>();
        String resourceUuId = serviceNode.getMetaData().getValue("UUID");
        String nodeTypeName = idTypeStore.get(resourceUuId);
        if (nodeTypeName != null) {
            Model resourceModel = Model.getModelFor(nodeTypeName, serviceNode.getMetaData().getValue("type"));

            log.debug("Processing resource " + nodeTypeName + ": " + resourceUuId);
            Map<String, String> serviceMetadata = serviceNode.getMetaData().getAllProperties();
            resourceModel.populateModelIdentificationInformation(serviceMetadata);

            idTypeStore.remove(resourceModel.getModelNameVersionId());
            processResourceModels(idTypeStore, resourceModel, resourceNodes);

            if (csarHelper.getServiceVfList() != null) {
                processVfModules(resources, resourceModel, serviceNode);
            }

            if (hasSubCategoryTunnelXConnect(serviceMetadata) && hasAllottedResource(serviceMetadata)) {
                resourceModel.addWidget(new TunnelXconnectWidget());
            }

            resources.addAll(processInstanceGroups(resourceModel, serviceNode));
            resources.add((Resource) resourceModel);
        }
        return resources;
    }

    /**
     * Process groups for this service node, according to the defined filter.
     *
     * @param resourceModel
     * @param serviceNode
     * @return resources for which XML Models should be generated
     */
    List<Resource> processInstanceGroups(Model resourceModel, NodeTemplate serviceNode) {
        List<Resource> resources = new ArrayList<>();
        if (csarHelper.getNodeTemplateByName(serviceNode.getName()).getSubMappingToscaTemplate() != null) {
            List<Group> serviceGroups = csarHelper.getGroupsOfOriginOfNodeTemplate(serviceNode);
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
     * Create an Instance Group Model and populate it with the supplied data.
     *
     * @param resourceModel the Resource node template Model
     * @param memberNodes the Resources and Widgets belonging to the Group
     * @param metaProperties the metadata of the Group
     * @param properties the properties of the Group
     * @return the Instance Group and Member resource models
     */
    private List<Resource> processInstanceGroup(Model resourceModel, ArrayList<NodeTemplate> memberNodes,
            Map<String, String> metaProperties, Map<String, Property> properties) {
        List<Resource> resources = new ArrayList<>();

        Resource groupModel = new InstanceGroup();
        groupModel.populateModelIdentificationInformation(metaProperties);
        groupModel.populateModelIdentificationInformation(populateStringProperties(properties));

        resourceModel.addResource(groupModel);
        resources.add(groupModel);

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

    /**
     * Add the supplied Node Template to the Service, provided that it is a valid Resource or Widget. If the Node
     * Template is a Resource type, this is also recorded in the supplied nodesById Map.
     *
     * @param nodesById a map of Resource node type names, keyed by UUID
     * @param service the Service to which the Node Template should be added
     * @param nodeTemplate the Node Template to add (only if this is a Resource or Widget type)
     */
    private void addNodeToService(Map<String, String> nodesById, Service service, NodeTemplate nodeTemplate) {
        String nodeTypeName = normaliseNodeTypeName(nodeTemplate);
        Model model = Model.getModelFor(nodeTypeName, nodeTemplate.getMetaData().getValue("type"));
        if (model != null) {
            if (nodeTemplate.getMetaData() != null) {
                model.populateModelIdentificationInformation(nodeTemplate.getMetaData().getAllProperties());
            }

            addRelatedModel(service, model);
            if (model instanceof Resource) {
                nodesById.put(model.getModelNameVersionId(), nodeTypeName);
            }
        }
    }

    /**
     * @param model
     * @param relation
     */
    private void addRelatedModel(final Model model, final Model relation) {
        if (relation instanceof Resource) {
            model.addResource((Resource) relation);
        } else {
            model.addWidget((Widget) relation);
        }
    }

    /**
     * Process TOSCA Group information for VF Modules.
     *
     * @param resources
     * @param model
     * @param serviceNode
     */
    private void processVfModules(List<Resource> resources, Model resourceModel, NodeTemplate serviceNode) {
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

    private void processVfModule(List<Resource> resources, Model model, Group groupDefinition, NodeTemplate serviceNode,
            VfModule groupModel) {
        // Populate group with metadata properties
        groupModel.populateModelIdentificationInformation(groupDefinition.getMetadata().getAllProperties());
        // Populate group with non-metadata properties
        Map<String, Property> groupProperties = groupDefinition.getProperties();
        Map<String, String> properties = populateStringProperties(groupProperties);
        groupModel.populateModelIdentificationInformation(properties);
        processVfModuleGroup(resources, model, groupDefinition, serviceNode, groupModel);
    }

    private void processVfModuleGroup(List<Resource> resources, Model model, Group groupDefinition,
            NodeTemplate serviceNode, VfModule groupModel) {
        // Get names of the members of the service group
        List<NodeTemplate> members = csarHelper.getMembersOfVfModule(serviceNode, groupDefinition);
        if (members != null && !members.isEmpty()) {
            List<String> memberNames = members.stream().map(NodeTemplate::getName).collect(Collectors.toList());
            groupModel.setMembers(memberNames);
            for (NodeTemplate member : members) {
                processGroupMembers(groupModel, member);
            }
        }

        model.addResource(groupModel); // Added group (VfModule) to the (VF) model
        // Check if we have already encountered the same VfModule across all the artifacts
        if (!resources.contains(groupModel)) {
            resources.add(groupModel);
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

    private String normaliseNodeTypeName(NodeTemplate nodeType) {
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

    private boolean hasAllottedResource(Map<String, String> metadata) {
        return ALLOTTED_RESOURCE.equals(metadata.get(CATEGORY));
    }

    private boolean hasSubCategoryTunnelXConnect(Map<String, String> metadata) {
        return TUNNEL_XCONNECT.equals(metadata.get(SUBCATEGORY));
    }

    /**
     * Create a Map of property name against String property value from the input Map
     *
     * @param inputMap The input Map
     * @return Map of property name against String property value
     */
    private Map<String, String> populateStringProperties(Map<String, Property> inputMap) {
        return inputMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().getValue() == null ? "" : e.getValue().getValue().toString()));
    }

    private void processResourceModels(Map<String, String> idTypeStore, Model resourceModel,
            List<NodeTemplate> resourceNodes) {
        boolean foundProvidingService = false;

        for (NodeTemplate resourceNodeTemplate : resourceNodes) {
            String nodeTypeName = normaliseNodeTypeName(resourceNodeTemplate);
            Metadata metaData = resourceNodeTemplate.getMetaData();
            String metaDataType = Optional.ofNullable(metaData).map(m -> m.getValue("type")).orElse(nodeTypeName);
            Model resourceNode = Model.getModelFor(nodeTypeName, metaDataType);
            foundProvidingService |= processModel(idTypeStore, resourceModel, resourceNodeTemplate, nodeTypeName,
                    metaData, resourceNode);
        }

        if (resourceModel instanceof AllotedResource && !foundProvidingService) {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_PROVIDING_SERVICE_MISSING, resourceModel.getModelId()));
        }
    }

    private boolean processModel(Map<String, String> idTypeStore, Model resourceModel,
            NodeTemplate resourceNodeTemplate, String nodeTypeName, Metadata metaData, Model resourceNode) {
        boolean foundProvidingService = false;
        if (resourceNode instanceof ProvidingService) {
            foundProvidingService = true;
            processProvidingService(resourceModel, resourceNodeTemplate, resourceNode);
        } else if (resourceNode instanceof Resource && !(resourceNode.getWidgetType().equals(Widget.Type.L3_NET))) {
            if (metaData != null) {
                resourceNode.populateModelIdentificationInformation(metaData.getAllProperties());
            }
            idTypeStore.put(resourceNode.getModelNameVersionId(), nodeTypeName);
            resourceModel.addResource((Resource) resourceNode);
        }
        return foundProvidingService;
    }

    private void processProvidingService(Model resourceModel, NodeTemplate resourceNodeTemplate, Model resourceNode) {
        Map<String, Property> nodeProperties = resourceNodeTemplate.getProperties();
        if (nodeProperties.get("providing_service_uuid") == null
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
