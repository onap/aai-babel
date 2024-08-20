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
package org.onap.aai.babel.csar.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.ModelGenerator;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.cl.api.Logger;

/**
 * This class extracts YAML files from CSAR (compressed archive) content.
 *
 */
public class YamlExtractor {
    private static Logger logger = LogHelper.INSTANCE;

    private static final Pattern YAMLFILE_EXTENSION_REGEX = Pattern.compile("(?i).*\\.ya?ml$");

    /**
     * This method is responsible for filtering the contents of the supplied archive and returning a collection of
     * {@link Artifact}s that represent the YAML files that have been found in the archive.
     *
     * @param archive
     *            the compressed archive in the form of a byte array, expected to contain one or more YAML files
     * @param name
     *            the name of the archive
     * @param version
     *            the version of the archive
     * @return List&lt;Artifact&gt; collection of YAML artifacts found in the archive
     * @throws InvalidArchiveException
     *             if an error occurs trying to extract the YAML file(s) from the archive, or no files were found
     */
    public List<Artifact> extract(byte[] archive, String name, String version) throws InvalidArchiveException {
        validateRequest(archive, name, version);

        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Extracting CSAR archive: " + name);

        List<Artifact> ymlFiles = new ArrayList<>();
        try (SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(archive);
             ZipFile zipFile = new ZipFile(inMemoryByteChannel)) {
            Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();
            while(enumeration.hasMoreElements()) {
                ZipArchiveEntry entry = enumeration.nextElement();
                if (fileShouldBeExtracted(entry)) {
                    Artifact artifact = ModelGenerator.createArtifact(IOUtils.toByteArray(zipFile.getInputStream(entry)),
                    entry.getName(), version);
                    ymlFiles.add(artifact);
                }
            }
            if (ymlFiles.isEmpty()) {
                throw new InvalidArchiveException("No valid YAML files were found in the CSAR file.");
            }
        } catch (IOException e) {
            throw new InvalidArchiveException(
                    "An error occurred trying to create a ZipFile. Is the content being converted really a CSAR file?",
                    e);
        }

        logger.debug(ApplicationMsgs.DISTRIBUTION_EVENT, ymlFiles.size() + " YAML files extracted.");

        return ymlFiles;
    }

    /**
     * Throw an error if the supplied parameters are not valid.
     *
     * @param archive
     * @param name
     * @param version
     * @throws InvalidArchiveException
     */
    private void validateRequest(byte[] archive, String name, String version) throws InvalidArchiveException {
        if (archive == null || archive.length == 0) {
            throw new InvalidArchiveException("An archive must be supplied for processing.");
        } else if (StringUtils.isBlank(name)) {
            throw new InvalidArchiveException("The name of the archive must be supplied for processing.");
        } else if (StringUtils.isBlank(version)) {
            throw new InvalidArchiveException("The version must be supplied for processing.");
        }
    }

    /**
     * Determine whether the file name matches the pattern for YAML content.
     *
     * @param entry
     *            the entry
     * @return true, if successful
     */
    private boolean fileShouldBeExtracted(ZipArchiveEntry entry) {
        boolean extractFile = YAMLFILE_EXTENSION_REGEX.matcher(entry.getName()).matches();
        logger.debug(ApplicationMsgs.DISTRIBUTION_EVENT, "Extraction of " + entry.getName() + "=" + extractFile);
        return extractFile;
    }
}
