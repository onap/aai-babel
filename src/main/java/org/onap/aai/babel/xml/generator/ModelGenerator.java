/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 European Software Marketing Ltd.
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
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.babel.xml.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.openecomp.sdc.generator.data.AdditionalParams;
import org.openecomp.sdc.generator.data.Artifact;
import org.openecomp.sdc.generator.data.GenerationData;
import org.openecomp.sdc.generator.data.GeneratorUtil;
import org.openecomp.sdc.generator.data.GroupType;
import org.openecomp.sdc.generator.service.ArtifactGenerationService;

/**
 * This class is responsible for generating xml model artifacts from a collection of csar file artifacts
 */
public class ModelGenerator implements ArtifactGenerator {

    private static Logger logger = LoggerFactory.getInstance().getLogger(ModelGenerator.class);

    private static final String GENERATORCONFIG = "{\"artifactTypes\": [\"AAI\"]}";
    private static final Pattern UUID_NORMATIVE_NEW_VERSION = Pattern.compile("^\\d{1,}.0");
    private static final String VERSION_DELIMETER = ".";
    private static final String VERSION_DELIMETER_REGEXP = "\\" + VERSION_DELIMETER;
    private static final String DEFAULT_SERVICE_VERSION = "1.0";

    /**
     * Invokes the TOSCA artifact generator API with the input artifacts.
     *
     * @param csarArtifacts the input artifacts
     * @return {@link List} of output artifacts
     * @throws XmlArtifactGenerationException if there is an error trying to generate xml artifacts
     */
    @Override
    public List<BabelArtifact> generateArtifacts(List<Artifact> csarArtifacts) throws XmlArtifactGenerationException {
        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT,
                "Generating XML for " + csarArtifacts.size() + " CSAR artifacts.");

        // Get the service version to pass into the generator
        String toscaVersion = csarArtifacts.get(0).getVersion();
        logger.debug(
                "Getting the service version for Tosca Version of the yml file.  The Tosca Version is " + toscaVersion);
        String serviceVersion = getServiceVersion(toscaVersion);
        logger.debug("The service version is " + serviceVersion);
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AdditionalParams.ServiceVersion.getName(), serviceVersion);

        // Call ArtifactGenerator API
        logger.debug("Obtaining instance of ArtifactGenerationService");
        ArtifactGenerationService generationService = ArtifactGenerationService.lookup();
        logger.debug("About to call generationService.generateArtifact()");
        GenerationData data = generationService.generateArtifact(csarArtifacts, GENERATORCONFIG, additionalParams);
        logger.debug("Call generationService.generateArtifact() has finished");

        // Convert results into BabelArtifacts
        if (data.getErrorData().isEmpty()) {
            return data.getResultData().stream().map(a -> new BabelArtifact(a.getName(), a.getType(), a.getPayload()))
                    .collect(Collectors.toList());
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
        String serviceVersion;

        try {
            if (UUID_NORMATIVE_NEW_VERSION.matcher(artifactVersion).matches()) {
                serviceVersion = artifactVersion;
            } else {
                String[] versionParts = artifactVersion.split(VERSION_DELIMETER_REGEXP);
                Integer majorVersion = Integer.parseInt(versionParts[0]);

                serviceVersion = (majorVersion + 1) + VERSION_DELIMETER + "0";
            }
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
