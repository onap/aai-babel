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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.parser.ToscaParser;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;
import org.onap.aai.babel.xml.generator.data.GroupType;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.babel.xml.generator.model.Widget;
import org.onap.aai.babel.xml.generator.model.WidgetType;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.IEntityDetails;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.elements.queries.EntityQuery;
import org.onap.sdc.tosca.parser.elements.queries.TopologyTemplateQuery;
import org.onap.sdc.tosca.parser.enums.SdcTypes;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.Property;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.slf4j.MDC;

public class AaiArtifactGenerator implements ArtifactGenerator {

    private static final String SDNC_MODEL_VERSION = "sdnc_model_version";
    private static final String SDNC_MODEL_NAME = "sdnc_model_name";
    private static Logger log = LogHelper.INSTANCE;

    private static final String MDC_PARAM_MODEL_INFO = "ARTIFACT_MODEL_INFO";
    private static final String GENERATOR_AAI_GENERATED_ARTIFACT_EXTENSION = "xml";
    private static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA =
            "Service tosca missing from list of input artifacts";
    private static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is not specified";
    private static final String GENERATOR_AAI_INVALID_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is incorrect";

    private AaiModelGenerator modelGenerator = new AaiModelGenerator();

    @Override
    public GenerationData generateArtifact(byte[] csarArchive, List<Artifact> input,
            Map<String, String> additionalParams) {
        String configLocation = System.getProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
        if (configLocation == null) {
            throw new IllegalArgumentException(
                    String.format(ArtifactGeneratorToscaParser.GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND,
                            ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE));
        }

        try {
            ArtifactGeneratorToscaParser.initToscaMappingsConfiguration(configLocation);
        } catch (IOException e) {
            log.error(ApplicationMsgs.LOAD_PROPERTIES, e, configLocation);
            return createErrorData(e);
        }

        Path csarPath;

        try {
            csarPath = createTempFile(csarArchive);
        } catch (IOException e) {
            log.error(ApplicationMsgs.TEMP_FILE_ERROR, e);
            return createErrorData(e);
        }

        try {
            ISdcCsarHelper csarHelper =
                    SdcToscaParserFactory.getInstance().getSdcCsarHelper(csarPath.toAbsolutePath().toString());
            return generateAllArtifacts(validateServiceVersion(additionalParams), csarHelper);
        } catch (SdcToscaParserException | ClassCastException | XmlArtifactGenerationException e) {
            log.error(ApplicationMsgs.INVALID_CSAR_FILE, e);
            return createErrorData(e);
        } finally {
            FileUtils.deleteQuietly(csarPath.toFile());
        }
    }

    private GenerationData createErrorData(Exception e) {
        GenerationData generationData = new GenerationData();
        generationData.add(ArtifactType.AAI.name(), e.getMessage());
        return generationData;
    }

    /**
     * Generate model artifacts for the Service and its associated Resources.
     *
     * @param serviceVersion
     * @param csarHelper
     *            interface to the TOSCA parser
     * @return the generated Artifacts (containing XML models)
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support processed widget type(s)
     */
    public GenerationData generateAllArtifacts(final String serviceVersion, ISdcCsarHelper csarHelper)
            throws XmlArtifactGenerationException {
        Service serviceModel = createServiceModel(serviceVersion, csarHelper.getServiceMetadataAllProperties());

        MDC.put(MDC_PARAM_MODEL_INFO, serviceModel.getModelName() + "," + getArtifactLabel(serviceModel));

        List<Resource> resources = generateResourceModels(csarHelper, serviceModel);

        // Generate the A&AI XML model for the Service.
        final String serviceArtifact = modelGenerator.generateModelFor(serviceModel);

        // Build a Babel Artifact to be returned to the caller.
        GenerationData generationData = new GenerationData();
        generationData.add(getServiceArtifact(serviceModel, serviceArtifact));

        // For each Resource, generate the A&AI XML model and then create an additional Artifact for that model.
        for (Resource resource : resources) {
            generateResourceArtifact(generationData, resource);
            for (Resource childResource : resource.getResources()) {
                boolean isProvidingService =
                        (boolean) Optional.ofNullable(childResource.getProperties().get("providingService")) //
                                .orElse(false);
                if (!isProvidingService) {
                    generateResourceArtifact(generationData, childResource);
                }
            }
        }

        return generationData;
    }

    /**
     * Create a Service from the provided metadata
     *
     * @param serviceVersion
     * @param properties
     * @return
     */
    private Service createServiceModel(final String serviceVersion, Map<String, String> properties) {
        log.debug("Processing (TOSCA) Service object");
        Service serviceModel = new Service();
        serviceModel.setModelVersion(serviceVersion);
        serviceModel.populateModelIdentificationInformation(properties);
        return serviceModel;
    }

    /**
     * @param csarHelper
     * @param serviceModel
     * @return the generated Models
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support processed widget type(s)
     */
    private List<Resource> generateResourceModels(ISdcCsarHelper csarHelper, Service serviceModel)
            throws XmlArtifactGenerationException {
        List<NodeTemplate> serviceNodeTemplates =
                ToscaParser.getServiceNodeTemplates(csarHelper).collect(Collectors.toList());
        if (serviceNodeTemplates == null) {
            throw new IllegalArgumentException(GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA);
        }

        final ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(csarHelper);
        List<Resource> resources = new ArrayList<>();
        final List<Group> serviceGroups = ToscaParser.getServiceLevelGroups(csarHelper);
        for (NodeTemplate nodeTemplate : serviceNodeTemplates) {
            if (nodeTemplate.getMetaData() != null) {
                generateModelFromNodeTemplate(csarHelper, serviceModel, resources, serviceGroups, parser, nodeTemplate);
            } else {
                log.warn(ApplicationMsgs.MISSING_SERVICE_METADATA, nodeTemplate.getName());
            }
        }

        return resources;
    }

    /**
     * @param csarHelper
     * @param serviceModel
     * @param resources
     * @param serviceGroups
     * @param parser
     * @param nodeTemplate
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support processed widget type(s)
     */
    private void generateModelFromNodeTemplate(ISdcCsarHelper csarHelper, Service serviceModel,
            List<Resource> resources, final List<Group> serviceGroups, ArtifactGeneratorToscaParser parser,
            NodeTemplate nodeTemplate) throws XmlArtifactGenerationException {
        Resource model = getModelFor(parser, nodeTemplate);

        if (model != null) {
            if (nodeTemplate.getMetaData() != null) {
                model.populateModelIdentificationInformation(nodeTemplate.getMetaData().getAllProperties());
            }

            parser.addRelatedModel(serviceModel, model);
            if (model.getModelType() == ModelType.RESOURCE) {
                generateResourceModel(csarHelper, resources, parser, nodeTemplate);
            }
        } else {
            for (Group group : serviceGroups) {
                ArrayList<String> members = group.getMembers();
                if (members != null && members.contains(nodeTemplate.getName())
                        && WidgetConfigurationUtil.isSupportedInstanceGroup(group.getType())) {
                    log.debug(String.format("Adding group %s (type %s) with members %s", group.getName(),
                            group.getType(), members));

                    Resource groupModel = parser.createInstanceGroupModel(
                            parser.mergeProperties(group.getMetadata().getAllProperties(), group.getProperties()));
                    serviceModel.addResource(groupModel);
                    resources.add(groupModel);
                }
            }
        }
    }

    private Resource getModelFor(ArtifactGeneratorToscaParser parser, NodeTemplate nodeTemplate) {
        String nodeTypeName = nodeTemplate.getType();

        log.debug("Processing resource " + nodeTypeName + ": " + nodeTemplate.getMetaData().getValue("UUID"));

        Resource model = Model.getModelFor(nodeTypeName, nodeTemplate.getMetaData().getValue("type"));

        if (model != null) {
            Metadata metadata = nodeTemplate.getMetaData();
            if (metadata != null && parser.hasAllottedResource(metadata.getAllProperties())
                    && model.hasWidgetType("VF")) {
                model = new Resource(WidgetType.valueOf("ALLOTTED_RESOURCE"), true);
            }
        }

        return model;
    }

    /**
     * @param csarHelper
     * @param resources
     * @param parser
     * @param serviceVfNode
     *            a VF resource Node Template
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support processed widget type(s)
     */
    private void generateResourceModel(ISdcCsarHelper csarHelper, List<Resource> resources,
            ArtifactGeneratorToscaParser parser, NodeTemplate serviceVfNode) throws XmlArtifactGenerationException {
        Resource resourceModel = getModelFor(parser, serviceVfNode);
        if (resourceModel == null) {
            log.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Could not generate resource model");
            return;
        }

        Map<String, String> serviceMetadata = serviceVfNode.getMetaData().getAllProperties();
        resourceModel.populateModelIdentificationInformation(serviceMetadata);

        Map<String, String> pnfProps = getResourceProperties(csarHelper, SdcTypes.PNF);
        resourceModel.populateModelIdentificationInformation(pnfProps);

        Map<String, String> vfProps = getResourceProperties(csarHelper, SdcTypes.VF);
        resourceModel.populateModelIdentificationInformation(vfProps);

        parser.processResourceModels(resourceModel, getNonVnfChildren(serviceVfNode));

        List<NodeTemplate> serviceVfList = ToscaParser.getServiceNodeTemplates(csarHelper)
                .filter(ToscaParser.filterOnType(SdcTypes.VF)).collect(Collectors.toList());

        if (serviceVfList != null) {
            parser.processVfModules(resources, resourceModel, serviceVfNode);
        }

        if (parser.hasSubCategoryTunnelXConnect(serviceMetadata) && parser.hasAllottedResource(serviceMetadata)) {
            resourceModel.addWidget(Widget.createWidget("TUNNEL_XCONNECT"));
        }

        resources.addAll(parser.processInstanceGroups(resourceModel, serviceVfNode));
        resources.add(resourceModel);
    }

    private Map<String, String> getResourceProperties(ISdcCsarHelper csarHelper, SdcTypes type) {
        EntityQuery entityQuery = EntityQuery.newBuilder(type).build();
        TopologyTemplateQuery topologyTemplateQuery = TopologyTemplateQuery.newBuilder(SdcTypes.SERVICE).build();
        List<IEntityDetails> entityDetailsList = csarHelper.getEntity(entityQuery, topologyTemplateQuery, false);
        Map<String, String> props = new HashMap<>();
        for (IEntityDetails entityDetails : entityDetailsList) {
            Map<String, Property> properties = entityDetails.getProperties();
            if (properties.get(SDNC_MODEL_VERSION) != null && properties.get(SDNC_MODEL_NAME) != null) {
                props.put(SDNC_MODEL_VERSION, String.valueOf(properties.get(SDNC_MODEL_VERSION).getValue()));
                props.put(SDNC_MODEL_NAME, String.valueOf(properties.get(SDNC_MODEL_NAME).getValue()));
            }
        }
        return props;
    }

    /**
     * Return all child Node Templates (via Substitution Mappings) that do not have a type ending VnfConfiguration.
     *
     * @param nodeTemplate
     *            the parent Node Template
     * @return the child Node Templates which are not a VNF Configuration type
     */
    private List<NodeTemplate> getNonVnfChildren(NodeTemplate nodeTemplate) {
        return Optional.ofNullable(nodeTemplate.getSubMappingToscaTemplate()) //
                .map(sm -> Optional.ofNullable(sm.getNodeTemplates())
                        .map(nts -> nts.stream().filter(nt -> !isVNFType(nt)) //
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList()))
                .orElse(Collections.emptyList());
    }

    private boolean isVNFType(NodeTemplate nt) {
        return nt.getType().endsWith("VnfConfiguration");
    }

    /**
     * @param generationData
     * @param resource
     * @throws XmlArtifactGenerationException
     */
    private void generateResourceArtifact(GenerationData generationData, Resource resource)
            throws XmlArtifactGenerationException {
        if (!isContained(generationData, getArtifactName(resource))) {
            log.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Generating resource model");
            generationData.add(getResourceArtifact(resource, modelGenerator.generateModelFor(resource)));
        }
    }

    private Path createTempFile(byte[] bytes) throws IOException {
        log.debug("Creating temp file on file system for the csar");
        Path path = Files.createTempFile("temp", ".csar");
        Files.write(path, bytes);
        return path;
    }

    /**
     * Create the artifact label for an AAI model.
     *
     * @param model
     * @return the artifact label as String
     */
    private String getArtifactLabel(Model model) {
        StringBuilder artifactName = new StringBuilder(ArtifactType.AAI.name());
        artifactName.append("-");
        artifactName.append(model.getModelTypeName());
        artifactName.append("-");
        artifactName.append(hashCodeUuId(model.getModelNameVersionId()));
        return (artifactName.toString()).replaceAll("[^a-zA-Z0-9 +]+", "-");
    }

    /**
     * Method to generate the artifact name for an AAI model.
     *
     * @param model
     *            AAI artifact model
     * @return Model artifact name
     */
    private String getArtifactName(Model model) {
        StringBuilder artifactName = new StringBuilder(ArtifactType.AAI.name());
        artifactName.append("-");

        String truncatedArtifactName = truncateName(model.getModelName());
        artifactName.append(truncatedArtifactName);

        artifactName.append("-");
        artifactName.append(model.getModelTypeName());
        artifactName.append("-");
        artifactName.append(model.getModelVersion());

        artifactName.append(".");
        artifactName.append(GENERATOR_AAI_GENERATED_ARTIFACT_EXTENSION);
        return artifactName.toString();
    }

    /**
     * Create Resource artifact model from the AAI xml model string.
     *
     * @param resourceModel
     *            Model of the resource artifact
     * @param aaiResourceModel
     *            AAI model as string
     * @return Generated {@link Artifact} model for the resource
     */
    private Artifact getResourceArtifact(Resource resourceModel, String aaiResourceModel) {
        final String resourceArtifactLabel = getArtifactLabel(resourceModel);
        MDC.put(MDC_PARAM_MODEL_INFO, resourceModel.getModelName() + "," + resourceArtifactLabel);
        final byte[] bytes = aaiResourceModel.getBytes();

        Artifact artifact = new Artifact(ArtifactType.MODEL_INVENTORY_PROFILE.name(), GroupType.DEPLOYMENT.name(),
                GeneratorUtil.checkSum(bytes), GeneratorUtil.encode(bytes));
        artifact.setName(getArtifactName(resourceModel));
        artifact.setLabel(resourceArtifactLabel);
        artifact.setDescription("AAI Resource Model");
        return artifact;
    }

    /**
     * @param generationData
     * @param artifactName
     * @return
     */
    private boolean isContained(GenerationData generationData, final String artifactName) {
        return generationData.getResultData().stream()
                .anyMatch(artifact -> StringUtils.equals(artifact.getName(), artifactName));
    }

    /**
     * Create Service artifact model from the AAI XML model.
     *
     * @param serviceModel
     *            Model of the service artifact
     * @param aaiServiceModel
     *            AAI model as string
     * @return Generated {@link Artifact} model for the service
     */
    private Artifact getServiceArtifact(Service serviceModel, String aaiServiceModel) {
        Artifact artifact = new Artifact(ArtifactType.MODEL_INVENTORY_PROFILE.name(), GroupType.DEPLOYMENT.name(),
                GeneratorUtil.checkSum(aaiServiceModel.getBytes()), GeneratorUtil.encode(aaiServiceModel.getBytes()));
        String serviceArtifactName = getArtifactName(serviceModel);
        String serviceArtifactLabel = getArtifactLabel(serviceModel);
        artifact.setName(serviceArtifactName);
        artifact.setLabel(serviceArtifactLabel);
        artifact.setDescription("AAI Service Model");
        return artifact;
    }

    private int hashCodeUuId(String uuId) {
        int hashcode = 0;
        if (uuId != null) {
            for (int i = 0; i < uuId.length(); i++) {
                hashcode = 31 * hashcode + uuId.charAt(i);
            }
        }
        return hashcode;
    }

    private String truncateName(String name) {
        String truncatedName = name;
        if (name != null && name.length() >= 200) {
            truncatedName = name.substring(0, 199);
        }
        return truncatedName;
    }

    private String validateServiceVersion(Map<String, String> additionalParams) {
        String serviceVersion = additionalParams.get(AdditionalParams.SERVICE_VERSION.getName());
        if (serviceVersion == null) {
            throw new IllegalArgumentException(GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION);
        } else {
            String versionRegex = "^\\d*\\.\\d*$";
            if (!(serviceVersion.matches(versionRegex))) {
                throw new IllegalArgumentException(String.format(GENERATOR_AAI_INVALID_SERVICE_VERSION));
            }
        }
        return serviceVersion;
    }
}
