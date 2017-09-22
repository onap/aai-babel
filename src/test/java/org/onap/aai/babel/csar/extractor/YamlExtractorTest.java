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
package org.onap.aai.babel.csar.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.openecomp.sdc.generator.data.Artifact;

/**
 * Tests @see YamlExtractor
 */
public class YamlExtractorTest {

    private static final String FOO = "foo";
    private static final String SOME_BYTES = "just some bytes that will pass the firsts validation";
    private static final String SUPPLY_AN_ARCHIVE = "An archive must be supplied for processing.";
    private static final String SUPPLY_NAME = "The name of the archive must be supplied for processing.";
    private static final String SUPPLY_VERSION = "The version must be supplied for processing.";

    @Test
    public void extract_nullContentSupplied() {
        invalidArgumentsTest(null, FOO, FOO, SUPPLY_AN_ARCHIVE);
    }

    private void invalidArgumentsTest(byte[] archive, String name, String version, String expectedErrorMessage) {
        try {
            YamlExtractor.extract(archive, name, version);
            fail("An instance of InvalidArchiveException should have been thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof InvalidArchiveException);
            assertEquals(expectedErrorMessage, ex.getLocalizedMessage());
        }
    }

    @Test
    public void extract_emptyContentSupplied() {
        invalidArgumentsTest(new byte[0], FOO, FOO, SUPPLY_AN_ARCHIVE);
    }

    @Test
    public void extract_nullNameSupplied() {
        invalidArgumentsTest(SOME_BYTES.getBytes(), null, FOO, SUPPLY_NAME);
    }

    @Test
    public void extract_blankNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "  \t  ", FOO,
                SUPPLY_NAME);
    }

    @Test
    public void extract_emptyNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "", FOO, SUPPLY_NAME);
    }

    @Test
    public void extract_nullVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, null,
                SUPPLY_VERSION);
    }

    @Test
    public void extract_blankVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, "  \t  ",
                SUPPLY_VERSION);
    }

    @Test
    public void extract_emptyVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, "",
                SUPPLY_VERSION);
    }

    @Test
    public void extract_invalidContentSupplied() {
        invalidArgumentsTest("This is a piece of nonsense and not a zip file".getBytes(), FOO, FOO,
                "An error occurred trying to create a ZipFile. Is the content being converted really a csar file?");
    }

    @Test
    public void extract_archiveContainsNoYmlFiles() throws IOException {
        try {
            YamlExtractor.extract(loadResource("compressedArtifacts/noYmlFilesArchive.zip"), "noYmlFilesArchive.zip",
                    "v1");
            fail("An instance of InvalidArchiveException should have been thrown.");
        } catch (Exception e) {
            assertTrue("An instance of InvalidArchiveException should have been thrown.",
                    e instanceof InvalidArchiveException);
            assertEquals("Incorrect message was returned", "No valid yml files were found in the csar file.",
                    e.getMessage());
        }
    }

    private byte[] loadResource(final String archiveName) throws IOException {
        return IOUtils.toByteArray(YamlExtractor.class.getClassLoader().getResource(archiveName));
    }

    @Test
    public void extract_archiveContainsThreeRelevantYmlFilesFromSdWanService()
            throws IOException, InvalidArchiveException {
        List<Artifact> ymlFiles =
                YamlExtractor.extract(loadResource("compressedArtifacts/service-SdWanServiceTest-csar.csar"),
                        "service-SdWanServiceTest-csar.csar", "v1");

        List<String> payloads = new ArrayList<>();
        payloads.add("ymlFiles/resource-SdWanTestVsp-template.yml");
        payloads.add("ymlFiles/resource-TunnelXconntest-template.yml");
        payloads.add("ymlFiles/service-SdWanServiceTest-template.yml");

        new ArtifactTestUtils().performYmlAsserts(ymlFiles, payloads);
    }
}

