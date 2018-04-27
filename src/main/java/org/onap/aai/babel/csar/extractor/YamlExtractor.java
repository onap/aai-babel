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
 * The purpose of this class is to process a .csar file in the form of a byte array and extract yaml files from it.
 *
 * A .csar file is a compressed archive like a zip file and this class will treat the byte array as it if were a zip
 * file.
 */
public class YamlExtractor {
    private static Logger logger = LogHelper.INSTANCE;

    private static final Pattern YAMLFILE_EXTENSION_REGEX = Pattern.compile("(?i).*\\.ya?ml$");

    /** Private constructor */
    private YamlExtractor() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * This method is responsible for filtering the contents of the supplied archive and returning a collection of
     * {@link Artifact}s that represent the yml files that have been found in the archive.<br>
     * <br>
     * If the archive contains no yml files it will return an empty list.<br>
     *
     * @param archive the zip file in the form of a byte array containing one or more yml files
     * @param name the name of the archive file
     * @param version the version of the archive file
     * @return List<Artifact> collection of yml files found in the archive
     * @throws InvalidArchiveException if an error occurs trying to extract the yml files from the archive, if the
     *         archive is not a zip file or there are no yml files
     */
    public static List<Artifact> extract(byte[] archive, String name, String version) throws InvalidArchiveException {
        validateRequest(archive, name, version);

        logger.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Extracting CSAR archive: " + name);

        List<Artifact> ymlFiles = new ArrayList<>();
        try (SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(archive);
                ZipFile zipFile = new ZipFile(inMemoryByteChannel)) {
            for (Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries(); enumeration.hasMoreElements();) {
                ZipArchiveEntry entry = enumeration.nextElement();
                if (fileShouldBeExtracted(entry)) {
                    ymlFiles.add(ModelGenerator.createArtifact(IOUtils.toByteArray(zipFile.getInputStream(entry)),
                            entry.getName(), version));
                }
            }
            if (ymlFiles.isEmpty()) {
                throw new InvalidArchiveException("No valid yml files were found in the csar file.");
            }
        } catch (IOException e) {
            throw new InvalidArchiveException(
                    "An error occurred trying to create a ZipFile. Is the content being converted really a csar file?",
                    e);
        }

        logger.debug(ApplicationMsgs.DISTRIBUTION_EVENT, ymlFiles.size() + " YAML files extracted.");

        return ymlFiles;
    }

    private static void validateRequest(byte[] archive, String name, String version) throws InvalidArchiveException {
        if (archive == null || archive.length == 0) {
            throw new InvalidArchiveException("An archive must be supplied for processing.");
        } else if (StringUtils.isBlank(name)) {
            throw new InvalidArchiveException("The name of the archive must be supplied for processing.");
        } else if (StringUtils.isBlank(version)) {
            throw new InvalidArchiveException("The version must be supplied for processing.");
        }
    }

    /**
     * @param entry
     * @return
     */
    private static boolean fileShouldBeExtracted(ZipArchiveEntry entry) {
        boolean extractFile = YAMLFILE_EXTENSION_REGEX.matcher(entry.getName()).matches();
        logger.debug(ApplicationMsgs.DISTRIBUTION_EVENT,
                "Checking if " + entry.getName() + " should be extracted... " + extractFile);
        return extractFile;
    }
}
