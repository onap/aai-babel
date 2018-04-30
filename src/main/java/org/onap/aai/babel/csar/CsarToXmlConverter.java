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
package org.onap.aai.babel.csar;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.time.StopWatch;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.csar.extractor.YamlExtractor;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.xml.generator.ModelGenerator;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.Artifact;

/**
 * This class is responsible for converting content in a csar archive into one or more xml artifacts.
 */
public class CsarToXmlConverter {
    private static final LogHelper logger = LogHelper.INSTANCE;

    /**
     * This method is responsible for extracting one or more yaml files from the given csarArtifact and then using them
     * to generate xml artifacts.
     *
     * @param csarArchive the artifact that contains the csar archive to generate xml artifacts from
     * @param name the name of the archive file
     * @param version the version of the archive file
     * @return List<org.onap.sdc.generator.data.Artifact> a list of generated xml artifacts
     * @throws CsarConverterException if there is an error either extracting the yaml files or generating xml artifacts
     */
    public List<BabelArtifact> generateXmlFromCsar(byte[] csarArchive, String name, String version)
            throws CsarConverterException {

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        validateArguments(csarArchive, name, version);

        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT,
                "Starting to process csarArchive to convert contents to xml artifacts");
        List<BabelArtifact> xmlArtifacts;

        try {
            logger.debug("Calling YamlExtractor to extract ymlFiles");
            List<Artifact> ymlFiles = YamlExtractor.extract(csarArchive, name, version);

            logger.debug("Calling XmlArtifactGenerator to generateXmlArtifacts");
            xmlArtifacts = new ModelGenerator().generateArtifacts(csarArchive, ymlFiles);

            logger.debug(xmlArtifacts.size() + " xml artifacts have been generated");
        } catch (InvalidArchiveException e) {
            throw new CsarConverterException(
                    "An error occurred trying to extract the yml files from the csar file : " + e);
        } catch (XmlArtifactGenerationException e) {
            throw new CsarConverterException(
                    "An error occurred trying to generate xml files from a collection of yml files : " + e);
        } finally {
            logger.logMetrics(stopwatch, LogHelper.getCallerMethodName(0));
        }

        return xmlArtifacts;
    }

    private void validateArguments(byte[] csarArchive, String name, String version) {
        Objects.requireNonNull(csarArchive);
        Objects.requireNonNull(name);
        Objects.requireNonNull(version);
    }
}
