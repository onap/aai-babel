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
package org.onap.aai.babel.xml.generator.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;
import org.onap.aai.babel.xml.generator.data.GroupType;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.ProvidingService;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.NodeTemplate;
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
        Path csarPath;

        try {
            csarPath = createTempFile(csarArchive);
        } catch (IOException e) {
            log.error(ApplicationMsgs.TEMP_FILE_ERROR, e);
            return createErrorData(e);
        }

        try {
            ArtifactGeneratorToscaParser.initWidgetConfiguration();
            ArtifactGeneratorToscaParser.initGroupFilterConfiguration();
            ISdcCsarHelper csarHelper =
                    SdcToscaParserFactory.getInstance().getSdcCsarHelper(csarPath.toAbsolutePath().toString());
            return generateService(validateServiceVersion(additionalParams), csarHelper);
        } catch (Exception e) {
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
     * @param csarHelper TOSCA parser
     * @return the generated Artifacts
     */
    private GenerationData generateService(final String serviceVersion, ISdcCsarHelper csarHelper) {
        List<NodeTemplate> serviceNodeTemplates = csarHelper.getServiceNodeTemplates();
        if (serviceNodeTemplates == null) {
            throw new IllegalArgumentException(GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA);
        }

        // Populate basic service model metadata
        Service serviceModel = new Service();
        serviceModel.setModelVersion(serviceVersion);
        serviceModel.populateModelIdentificationInformation(csarHelper.getServiceMetadataAllProperties());

        Map<String, String> idTypeStore = new HashMap<>();

        ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(csarHelper);
        if (!serviceNodeTemplates.isEmpty()) {
            parser.processServiceTosca(serviceModel, idTypeStore, serviceNodeTemplates);
        }

        // Process the resource TOSCA files
        List<Resource> resources = parser.processResourceToscas(serviceNodeTemplates, idTypeStore);

        MDC.put(MDC_PARAM_MODEL_INFO, serviceModel.getModelName() + "," + getArtifactLabel(serviceModel));
        String aaiServiceModel = modelGenerator.generateModelFor(serviceModel);

        GenerationData generationData = new GenerationData();
        generationData.add(getServiceArtifact(serviceModel, aaiServiceModel));

        // Generate AAI XML resource model
        for (Resource resource : resources) {
            generateResourceArtifact(generationData, resource);
            for (Resource childResource : resource.getResources()) {
                if (!(childResource instanceof ProvidingService)) {
                    generateResourceArtifact(generationData, childResource);
                }
            }
        }

        return generationData;
    }

    /**
     * @param generationData
     * @param resource
     */
    private void generateResourceArtifact(GenerationData generationData, Resource resource) {
        if (!isContained(generationData, getArtifactName(resource))) {
            log.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Generating resource model");
            Artifact resourceArtifact = getResourceArtifact(resource, modelGenerator.generateModelFor(resource));
            generationData.add(resourceArtifact);
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
     * @param model AAI artifact model
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
     * @param resourceModel Model of the resource artifact
     * @param aaiResourceModel AAI model as string
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
     * Create Service artifact model from the AAI xml model string.
     *
     * @param serviceModel Model of the service artifact
     * @param aaiServiceModel AAI model as string
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
