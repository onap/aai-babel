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
package org.onap.aai.babel.xml.generator;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.babel.xml.generator.api.AaiArtifactGenerator;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.GenerationData;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;
import org.onap.aai.babel.xml.generator.data.GroupType;
import org.onap.aai.cl.api.Logger;

/**
 * This class is responsible for generating XML model artifacts from a collection of CSAR artifacts.
 */
public class ModelGenerator implements ArtifactGenerator {

    private static final Logger logger = LogHelper.INSTANCE;

    private static final String VERSION_DELIMITER = ".";
    private static final String VERSION_DELIMITER_REGEXP = "\\" + VERSION_DELIMITER;
    private static final String DEFAULT_SERVICE_VERSION = "1.0";

    /**
     * Invokes the TOSCA artifact generator API with the input artifacts.
     *
     * @param csarArchive
     * @param csarArtifacts the input artifacts
     * @return {@link List} of output artifacts
     * @throws XmlArtifactGenerationException if there is an error trying to generate XML artifacts
     */
    @Override
    public List<BabelArtifact> generateArtifacts(byte[] csarArchive, List<Artifact> csarArtifacts)
            throws XmlArtifactGenerationException {
        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT,
                "Generating XML for " + csarArtifacts.size() + " CSAR artifacts.");

        // Get the service version to pass into the generator
        String toscaVersion = csarArtifacts.get(0).getVersion();
        String serviceVersion = getServiceVersion(toscaVersion);
        logger.debug("The service version is " + serviceVersion);
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AdditionalParams.SERVICE_VERSION.getName(), serviceVersion);

        // Call ArtifactGenerator API
        logger.debug("Obtaining instance of ArtifactGenerationService");
        org.onap.aai.babel.xml.generator.api.ArtifactGenerator generator = new AaiArtifactGenerator();
        logger.debug("About to call generationService.generateArtifact()");
        GenerationData data = generator.generateArtifact(csarArchive, csarArtifacts, additionalParams);
        logger.debug("Call generationService.generateArtifact() has finished");

        // Convert results into BabelArtifacts
        if (data.getErrorData().isEmpty()) {
            return data.getResultData().stream().map(a -> new BabelArtifact(a.getName(), ArtifactType.MODEL,
                    new String(Base64.getDecoder().decode(a.getPayload())))).collect(Collectors.toList());
        } else {
            throw new XmlArtifactGenerationException(
                    "Error occurred during artifact generation: " + data.getErrorData().toString());
        }
    }

    /**
     * Creates an instance of an input artifact for the generator.
     *
     * @param payload the payload downloaded from SDC
     * @param artifactName name of the artifact to create
     * @param artifactVersion version of the artifact to create
     * @return an {@link Artifact} object constructed from the payload and artifactInfo
     */
    public static Artifact createArtifact(byte[] payload, String artifactName, String artifactVersion) {
        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Creating artifact for: " + artifactName);

        // Convert payload into an input Artifact
        String checksum = GeneratorUtil.checkSum(payload);
        byte[] encodedPayload = GeneratorUtil.encode(payload);
        Artifact artifact = new Artifact("TOSCA", GroupType.DEPLOYMENT.name(), checksum, encodedPayload);
        artifact.setName(artifactName);
        artifact.setLabel(artifactName);
        artifact.setDescription(artifactName);
        artifact.setVersion(artifactVersion);
        return artifact;
    }

    private static String getServiceVersion(String artifactVersion) {
        logger.debug("Artifact version=" + artifactVersion);
        String serviceVersion;
        try {
            int majorVersion = Integer.parseInt(artifactVersion.split(VERSION_DELIMITER_REGEXP)[0]);
            serviceVersion = majorVersion + VERSION_DELIMITER + "0";
        } catch (Exception e) {
            logger.warn(ApplicationMsgs.DISTRIBUTION_EVENT,
                    "Error generating service version from artifact version: " + artifactVersion
                            + ". Using default service version of: " + DEFAULT_SERVICE_VERSION + ". Error details: "
                            + e);
            return DEFAULT_SERVICE_VERSION;
        }

        return serviceVersion;
    }
}
