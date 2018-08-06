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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.AllotedResource;
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

public class ArtifactGeneratorToscaParser {

    private static Logger log = LogHelper.INSTANCE;

    public static final String PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE = "artifactgenerator.config";

    private static final String GENERATOR_AAI_CONFIGFILE_NOT_FOUND =
            "Cannot generate artifacts. Artifact Generator Configuration file not found at %s";
    private static final String GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND =
            "Cannot generate artifacts. artifactgenerator.config system property not configured";
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
            throw new IllegalArgumentException(GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND);
        }
    }

    /**
     * Generates a Resource List using input Service Node Templates.
     *
     * @param serviceNodes input Service Node Templates
     * @param idTypeStore ID->Type mapping
     * @return the processed resource models
     */
    public List<Resource> processResourceToscas(List<NodeTemplate> serviceNodes, Map<String, String> idTypeStore) {
        List<Resource> resources = new LinkedList<>();
        for (NodeTemplate serviceNode : serviceNodes) {
            List<NodeTemplate> resourceNodes = csarHelper.getNodeTemplateChildren(serviceNode);

            String resourceUuId = serviceNode.getMetaData().getValue("UUID");
            String mapValue = idTypeStore.get(resourceUuId);
            if (mapValue != null) {
                Model model = Model.getModelFor(idTypeStore.get(serviceNode.getMetaData().getValue("UUID")));

                log.debug("Inside Resource artifact generation for resource");
                Map<String, String> serviceMetadata = serviceNode.getMetaData().getAllProperties();
                model.populateModelIdentificationInformation(serviceMetadata);

                // Found model from the type store so removing the same
                idTypeStore.remove(model.getModelNameVersionId());
                processVfTosca(idTypeStore, model, resourceNodes);

                // Process group information from tosca for vfModules
                if (csarHelper.getServiceVfList() != null) {
                    processVfModules(resources, model, serviceNode);
                }

                if (hasSubCategoryTunnelXConnect(serviceMetadata) && hasAllottedResource(serviceMetadata)) {
                    model.addWidget(new TunnelXconnectWidget());
                }
                resources.add((Resource) model);
            }
        }
        return resources;
    }

    private void processVfModules(List<Resource> resources, Model model, NodeTemplate serviceNode) {
        // Get the customisation UUID for each VF node and use it to get its Groups
        String uuid = csarHelper.getNodeTemplateCustomizationUuid(serviceNode);

        // Populate a Map of Group against NodeTemplates that are members of the Group
        List<Group> serviceGroups = csarHelper.getVfModulesByVf(uuid);

        // Process each VF Group
        for (Group serviceGroup : serviceGroups) {
            Model groupModel = Model.getModelFor(serviceGroup.getType());
            if (groupModel instanceof VfModule) {
                processVfModule(resources, model, serviceGroup, serviceNode, (VfModule) groupModel);
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
            for (NodeTemplate nodeTemplate : members) {
                processNodeTemplate(groupModel, nodeTemplate);
            }
        }

        model.addResource(groupModel); // Added group (VfModule) to the (VF) model
        // Check if we have already encountered the same VfModule across all the artifacts
        if (!resources.contains(groupModel)) {
            resources.add(groupModel);
        }
    }

    private static void processNodeTemplate(Model group, NodeTemplate nodeTemplate) {
        Model resourceNode;
        // L3-network inside vf-module to be generated as Widget a special handling.
        if (nodeTemplate.getType().contains("org.openecomp.resource.vl")) {
            resourceNode = new L3NetworkWidget();
        } else {
            resourceNode = Model.getModelFor(nodeTemplate.getType());
        }
        if (resourceNode != null && !(resourceNode instanceof Resource)) {
            Widget widget = (Widget) resourceNode;
            widget.addKey(nodeTemplate.getName());
            // Add the widget element encountered to the Group model
            group.addWidget(widget);
        }
    }

    /**
     * Process the service tosca
     *
     * @param service model of the service artifact
     * @param idTypeStore ID->Type mapping
     * @param nodeTemplates a list of service nodes
     *
     */
    public void processServiceTosca(Service service, Map<String, String> idTypeStore,
            List<NodeTemplate> nodeTemplates) {
        log.debug("Inside Service Tosca ");
        // Get the resource/widgets in the service according to the node-template types
        for (NodeTemplate node : nodeTemplates) {
            Model model = Model.getModelFor(correctNodeType(node));
            if (model != null) {
                model.populateModelIdentificationInformation(node.getMetaData().getAllProperties());
                if (model instanceof Resource) {
                    // Keeping track of resource types and
                    // their uuid for identification during resource tosca processing
                    idTypeStore.put(model.getModelNameVersionId(), correctNodeType(node));
                    service.addResource((Resource) model);
                } else {
                    service.addWidget((Widget) model);
                }
            }
        }
    }

    private String correctNodeType(NodeTemplate nodeType) {
        String correctedNodeType = nodeType.getType();
        if (hasAllottedResource(nodeType.getMetaData().getAllProperties())) {
            if (nodeType.getType().contains("org.openecomp.resource.vf.")) {
                correctedNodeType = "org.openecomp.resource.vf.allottedResource";
            }
            if (nodeType.getType().contains("org.openecomp.resource.vfc.")) {
                correctedNodeType = "org.openecomp.resource.vfc.AllottedResource";
            }
        }
        return correctedNodeType;
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

    private void processVfTosca(Map<String, String> idTypeStore, Model model, List<NodeTemplate> resourceNodes) {
        boolean flag = false;

        for (NodeTemplate resourceNodeTemplate : resourceNodes) {
            Model resourceNode = Model.getModelFor(correctNodeType(resourceNodeTemplate));
            if (resourceNode instanceof ProvidingService) {
                flag = true;
                Map<String, Property> nodeProperties = resourceNodeTemplate.getProperties();
                if (nodeProperties.get("providing_service_uuid") == null
                        || nodeProperties.get("providing_service_invariant_uuid") == null) {
                    throw new IllegalArgumentException(
                            String.format(GENERATOR_AAI_PROVIDING_SERVICE_METADATA_MISSING, model.getModelId()));
                }
                Map<String, String> properties = populateStringProperties(nodeProperties);
                properties.put(VERSION, "1.0");
                resourceNode.populateModelIdentificationInformation(properties);
                model.addResource((Resource) resourceNode);
            } else if (resourceNode instanceof Resource && !(resourceNode.getWidgetType().equals(Widget.Type.L3_NET))) {
                idTypeStore.put(resourceNode.getModelNameVersionId(), correctNodeType(resourceNodeTemplate));
                model.addResource((Resource) resourceNode);
            }
        }

        if (model instanceof AllotedResource && !flag) {
            throw new IllegalArgumentException(
                    String.format(GENERATOR_AAI_PROVIDING_SERVICE_MISSING, model.getModelId()));
        }
    }
}
