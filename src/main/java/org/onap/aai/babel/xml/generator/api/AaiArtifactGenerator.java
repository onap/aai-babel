/**
 * ﻿============LICENSE_START=======================================================
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
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.data.GeneratorConstants;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;
import org.onap.aai.babel.xml.generator.data.GroupType;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.cl.api.Logger;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.slf4j.MDC;

public class AaiArtifactGenerator implements ArtifactGenerator {

    private static final String ARTIFACT_MODEL_INFO = "ARTIFACT_MODEL_INFO";

    private static Logger log = LogHelper.INSTANCE;

    @Override
    public GenerationData generateArtifact(byte[] csarArchive, List<Artifact> input,
            Map<String, String> additionalParams) {
        Path path = null;

        try {
            ArtifactGeneratorToscaParser.initWidgetConfiguration();
            String serviceVersion = validateServiceVersion(additionalParams);
            GenerationData generationData = new GenerationData();

            path = createTempFile(csarArchive);
            if (path != null) {
                ISdcCsarHelper csarHelper =
                        SdcToscaParserFactory.getInstance().getSdcCsarHelper(path.toAbsolutePath().toString());

                List<NodeTemplate> serviceNodes =
                        csarHelper.getServiceNodeTemplates();
                Map<String, String> serviceMetaData = csarHelper.getServiceMetadataAllProperties();

                if (serviceNodes == null) {
                    throw new IllegalArgumentException(GeneratorConstants.GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA);
                }

                // Populate basic service model metadata
                Service serviceModel = new Service();
                serviceModel.populateModelIdentificationInformation(serviceMetaData);
                serviceModel.setModelVersion(serviceVersion);

                Map<String, String> idTypeStore = new HashMap<>();

                ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(csarHelper);
                if (!serviceNodes.isEmpty()) {
                    parser.processServiceTosca(serviceModel, idTypeStore, serviceNodes);
                }

                // Process the resource TOSCA files
                List<Resource> resources = parser.processResourceToscas(serviceNodes, idTypeStore);

                // Generate AAI XML service model
                AaiModelGenerator modelGenerator = AaiModelGenerator.getInstance();
                MDC.put(ARTIFACT_MODEL_INFO, serviceModel.getModelName() + "," + getArtifactLabel(serviceModel));
                String aaiServiceModel = modelGenerator.generateModelFor(serviceModel);
                generationData.add(getServiceArtifact(serviceModel, aaiServiceModel));

                // Generate AAI XML resource model
                for (Resource res : resources) {
                    MDC.put(ARTIFACT_MODEL_INFO, res.getModelName() + "," + getArtifactLabel(res));
                    String aaiResourceModel = modelGenerator.generateModelFor(res);
                    generationData.add(getResourceArtifact(res, aaiResourceModel));

                }
            }
            return generationData;
        } catch (Exception e) {
            log.error(ApplicationMsgs.INVALID_CSAR_FILE, e);
            GenerationData generationData = new GenerationData();
            generationData.add(ArtifactType.AAI.name(), e.getMessage());
            return generationData;

        } finally {
            if (path != null) {
                FileUtils.deleteQuietly(path.toFile());
            }
        }
    }

    private Path createTempFile(byte[] bytes) {
        Path path = null;
        try {
            log.debug("Creating temp file on file system for the csar");
            path = Files.createTempFile("temp", ".csar");
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error(ApplicationMsgs.TEMP_FILE_ERROR, e);
        }
        return path;
    }

    /**
     * Method to generate the artifact label for AAI model
     *
     * @param model
     * @return the artifact label as String
     */
    public String getArtifactLabel(Model model) {
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
        artifactName.append(GeneratorConstants.GENERATOR_AAI_GENERATED_ARTIFACT_EXTENSION);
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
        Artifact artifact = new Artifact(ArtifactType.MODEL_INVENTORY_PROFILE.name(), GroupType.DEPLOYMENT.name(),
                GeneratorUtil.checkSum(aaiResourceModel.getBytes()), GeneratorUtil.encode(aaiResourceModel.getBytes()));
        String resourceArtifactName = getArtifactName(resourceModel);
        String resourceArtifactLabel = getArtifactLabel(resourceModel);
        artifact.setName(resourceArtifactName);
        artifact.setLabel(resourceArtifactLabel);
        String description = ArtifactGeneratorToscaParser.getArtifactDescription(resourceModel);
        artifact.setDescription(description);
        return artifact;
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
        String serviceVersion;
        serviceVersion = additionalParams.get(AdditionalParams.SERVICE_VERSION.getName());
        if (serviceVersion == null) {
            throw new IllegalArgumentException(GeneratorConstants.GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION);
        } else {
            String versionRegex = "^[1-9]\\d*(\\.0)$";
            if (!(serviceVersion.matches(versionRegex))) {
                throw new IllegalArgumentException(
                        String.format(GeneratorConstants.GENERATOR_AAI_INVALID_SERVICE_VERSION));
            }
        }
        return serviceVersion;
    }
}
