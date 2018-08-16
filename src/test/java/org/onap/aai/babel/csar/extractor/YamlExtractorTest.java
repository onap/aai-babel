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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.data.Artifact;

/**
 * Tests {@link YamlExtractor}.
 */
public class YamlExtractorTest {

    private static final String FOO = "foo";
    private static final String SOME_BYTES = "just some bytes that will pass the firsts validation";
    private static final String SUPPLY_AN_ARCHIVE = "An archive must be supplied for processing.";
    private static final String SUPPLY_NAME = "The name of the archive must be supplied for processing.";
    private static final String SUPPLY_VERSION = "The version must be supplied for processing.";

    @Test
    public void testNullContentSupplied() {
        invalidArgumentsTest(null, FOO, FOO, SUPPLY_AN_ARCHIVE);
    }

    @Test
    public void testEmptyContentSupplied() {
        invalidArgumentsTest(new byte[0], FOO, FOO, SUPPLY_AN_ARCHIVE);
    }

    @Test
    public void testNullNameSupplied() {
        invalidArgumentsTest(SOME_BYTES.getBytes(), null, FOO, SUPPLY_NAME);
    }

    @Test
    public void testBlankNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "  \t  ", FOO,
                SUPPLY_NAME);
    }

    @Test
    public void testEmptyNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "", FOO, SUPPLY_NAME);
    }

    @Test
    public void testNullVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, null,
                SUPPLY_VERSION);
    }

    @Test
    public void testBlankVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, "  \t  ",
                SUPPLY_VERSION);
    }

    @Test
    public void testEmptyVersionSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), FOO, "",
                SUPPLY_VERSION);
    }

    @Test
    public void testInvalidContentSupplied() {
        invalidArgumentsTest("This is a piece of nonsense and not a zip file".getBytes(), FOO, FOO,
                "An error occurred trying to create a ZipFile. Is the content being converted really a csar file?");
    }

    @Test
    public void testArchiveContainsNoYmlFiles() throws IOException {
        try {
            extractArchive("noYmlFilesArchive.zip");
            fail("An instance of InvalidArchiveException should have been thrown.");
        } catch (Exception e) {
            assertTrue("An instance of InvalidArchiveException should have been thrown.",
                    e instanceof InvalidArchiveException);
            assertEquals("Incorrect message was returned", "No valid YAML files were found in the csar file.",
                    e.getMessage());
        }
    }

    @Test
    public void testArchiveContainsOnlyTheExpectedYmlFilesFromSdWanService()
            throws IOException, InvalidArchiveException {
        final List<Artifact> ymlFiles = extractArchive("service-SdWanServiceTest-csar.csar");
        List<String> payloads = new ArrayList<>();
        payloads.add("ymlFiles/resource-SdWanTestVsp-template.yml");
        payloads.add("ymlFiles/resource-SdWanTestVsp-template-interface.yml");
        payloads.add("ymlFiles/resource-TunnelXconntest-template.yml");
        payloads.add("ymlFiles/resource-TunnelXconntest-template-interface.yml");
        payloads.add("ymlFiles/service-SdWanServiceTest-template.yml");
        payloads.add("ymlFiles/service-SdWanServiceTest-template-interface.yml");
        payloads.add("ymlFiles/resource-Allotedresource-template.yml");
        payloads.add("ymlFiles/resource-SdwantestvspNodesDummyServer-template.yml");
        payloads.add("ymlFiles/nodes.yml");
        payloads.add("ymlFiles/capabilities.yml");
        payloads.add("ymlFiles/artifacts.yml");
        payloads.add("ymlFiles/data.yml");
        payloads.add("ymlFiles/groups.yml");

        new ArtifactTestUtils().performYmlAsserts(ymlFiles, payloads);
    }

    /**
     * Call the extractor with the specified arguments and assert that an exception is thrown.
     *
     * @param archive
     * @param name
     * @param version
     * @param expectedErrorMessage
     */
    private void invalidArgumentsTest(byte[] archive, String name, String version, String expectedErrorMessage) {
        try {
            new YamlExtractor().extract(archive, name, version);
            fail("An instance of InvalidArchiveException should have been thrown");
        } catch (InvalidArchiveException ex) {
            assertTrue(ex instanceof InvalidArchiveException);
            assertEquals(expectedErrorMessage, ex.getLocalizedMessage());
        }
    }

    /**
     * Extract Artifacts from the specified CSAR resource.
     *
     * @param resourceName
     *            the CSAR file
     * @return the extracted artifacts
     * @throws InvalidArchiveException
     * @throws IOException
     *             for I/O errors
     */
    private List<Artifact> extractArchive(String resourceName) throws InvalidArchiveException, IOException {
        byte[] csar = new ArtifactTestUtils().getCompressedArtifact(resourceName);
        return new YamlExtractor().extract(csar, resourceName, "v1");
    }
}
