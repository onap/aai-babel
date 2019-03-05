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

package org.onap.aai.babel.xml.generator.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
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
import org.onap.aai.babel.xml.generator.model.Widget.Type;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.slf4j.MDC;

public class AaiArtifactGenerator implements ArtifactGenerator {

    private static Logger log = LogHelper.INSTANCE;

    private static final String MDC_PARAM_MODEL_INFO = "ARTIFACT_MODEL_INFO";
    private static final String GENERATOR_AAI_GENERATED_ARTIFACT_EXTENSION = "xml";
    private static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA =
            "Service tosca missing from list of input artifacts";
    private static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is not specified";
    private static final String GENERATOR_AAI_INVALID_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is incorrect";

    private AaiModelGenerator modelGenerator = new AaiModelGeneratorImpl();

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
            ArtifactGeneratorToscaParser.initWidgetConfiguration();
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
        } catch (SdcToscaParserException | XmlArtifactGenerationException e) {
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
     */
    private GenerationData generateAllArtifacts(final String serviceVersion, ISdcCsarHelper csarHelper)
            throws XmlArtifactGenerationException {
        List<NodeTemplate> serviceNodeTemplates = csarHelper.getServiceNodeTemplates();
        if (serviceNodeTemplates == null) {
            throw new IllegalArgumentException(GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA);
        }

        Service serviceModel = createServiceModel(serviceVersion, csarHelper.getServiceMetadataAllProperties());

        MDC.put(MDC_PARAM_MODEL_INFO, serviceModel.getModelName() + "," + getArtifactLabel(serviceModel));

        List<Resource> resources = generateResourceModels(csarHelper, serviceNodeTemplates, serviceModel);

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
     * @param serviceNodeTemplates
     * @param serviceModel
     * @return the generated Models
     * @throws XmlArtifactGenerationException
     */
    private List<Resource> generateResourceModels(ISdcCsarHelper csarHelper, List<NodeTemplate> serviceNodeTemplates,
            Service serviceModel) throws XmlArtifactGenerationException {
        final List<Group> serviceGroups = csarHelper.getGroupsOfTopologyTemplate();
        final ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(csarHelper);

        List<Resource> resources = new ArrayList<>();

        for (NodeTemplate nodeTemplate : serviceNodeTemplates) {
            if (nodeTemplate.getMetaData() != null) {
                generateModelFromNodeTemplate(csarHelper, serviceModel, resources, serviceGroups, parser, nodeTemplate);
            } else {
                log.warn(ApplicationMsgs.MISSING_SERVICE_METADATA, nodeTemplate.getName());
            }
        }

        return resources;
    }

    private void generateModelFromNodeTemplate(ISdcCsarHelper csarHelper, Service serviceModel,
            List<Resource> resources, final List<Group> serviceGroups, ArtifactGeneratorToscaParser parser,
            NodeTemplate nodeTemplate) throws XmlArtifactGenerationException {
        Resource model = getModelFor(parser, nodeTemplate);

        if (model != null) {
            if (nodeTemplate.getMetaData() != null) {
                model.populateModelIdentificationInformation(nodeTemplate.getMetaData().getAllProperties());
            }

            parser.addRelatedModel(serviceModel, model);
            if (model.isResource()) {
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
                    && model.getWidgetType() == Type.VF) {
                model = new Resource(Type.ALLOTTED_RESOURCE, true);
            }
        }

        return model;
    }

    private void generateResourceModel(ISdcCsarHelper csarHelper, List<Resource> resources,
            ArtifactGeneratorToscaParser parser, NodeTemplate nodeTemplate) throws XmlArtifactGenerationException {
        Resource resourceModel = getModelFor(parser, nodeTemplate);
        if (resourceModel == null) {
            log.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Could not generate resource model");
            return;
        }

        Map<String, String> serviceMetadata = nodeTemplate.getMetaData().getAllProperties();
        resourceModel.populateModelIdentificationInformation(serviceMetadata);

        parser.processResourceModels(resourceModel, csarHelper.getNodeTemplateChildren(nodeTemplate));

        if (csarHelper.getServiceVfList() != null) {
            parser.processVfModules(resources, resourceModel, nodeTemplate);
        }

        if (parser.hasSubCategoryTunnelXConnect(serviceMetadata) && parser.hasAllottedResource(serviceMetadata)) {
            resourceModel.addWidget(Widget.getWidget(Type.TUNNEL_XCONNECT));
        }

        resources.addAll(parser.processInstanceGroups(resourceModel, nodeTemplate));
        resources.add((Resource) resourceModel);
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
        artifactName.append(model.getModelType().name().toLowerCase());
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
        artifactName.append(model.getModelType().name().toLowerCase());
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
    private Artifact getResourceArtifact(Model resourceModel, String aaiResourceModel) {
        final String resourceArtifactLabel = getArtifactLabel(resourceModel);
        MDC.put(MDC_PARAM_MODEL_INFO, resourceModel.getModelName() + "," + resourceArtifactLabel);
        final byte[] bytes = aaiResourceModel.getBytes();

        Artifact artifact = new Artifact(ArtifactType.MODEL_INVENTORY_PROFILE.name(), GroupType.DEPLOYMENT.name(),
                GeneratorUtil.checkSum(bytes), GeneratorUtil.encode(bytes));
        artifact.setName(getArtifactName(resourceModel));
        artifact.setLabel(resourceArtifactLabel);
        artifact.setDescription(ArtifactGeneratorToscaParser.getArtifactDescription(resourceModel));
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
        String description = ArtifactGeneratorToscaParser.getArtifactDescription(serviceModel);
        artifact.setDescription(description);
        return artifact;
    }

    private int hashCodeUuId(String uuId) {
        int hashcode = 0;
        for (int i = 0; i < uuId.length(); i++) {
            hashcode = 31 * hashcode + uuId.charAt(i);
        }
        return hashcode;
    }

    private String truncateName(String name) {
        String truncatedName = name;
        if (name.length() >= 200) {
            truncatedName = name.substring(0, 199);
        }
        return truncatedName;
    }

    private String validateServiceVersion(Map<String, String> additionalParams) {
        String serviceVersion = additionalParams.get(AdditionalParams.SERVICE_VERSION.getName());
        if (serviceVersion == null) {
            throw new IllegalArgumentException(GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION);
        } else {
            String versionRegex = "^[1-9]\\d*(\\.0)$";
            if (!(serviceVersion.matches(versionRegex))) {
                throw new IllegalArgumentException(String.format(GENERATOR_AAI_INVALID_SERVICE_VERSION));
            }
        }
        return serviceVersion;
    }
}
