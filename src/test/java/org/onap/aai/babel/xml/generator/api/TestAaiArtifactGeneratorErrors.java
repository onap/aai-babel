/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.util.Resources;
import org.onap.aai.babel.xml.generator.data.AdditionalParams;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.GenerationData;

/**
 * Tests the configuration error paths of {@link AaiArtifactGenerator#generateArtifact}.
 */
public class TestAaiArtifactGeneratorErrors {

    private String originalConfigLocation;

    @BeforeEach
    public void saveSystemProperty() {
        originalConfigLocation = System.getProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
    }

    @AfterEach
    public void restoreSystemProperty() {
        if (originalConfigLocation == null) {
            System.clearProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
        } else {
            System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE, originalConfigLocation);
        }
    }

    @Test
    public void testMissingConfigLocationProperty() {
        System.clearProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
        assertThrows(IllegalArgumentException.class, () -> generateArtifact());
    }

    /**
     * A configured path that does not exist is rejected before the mappings are read, so this is thrown rather than
     * being reported as error data.
     */
    @Test
    public void testNonExistentConfigLocation() {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE,
                "/does/not/exist/tosca-mappings.json");
        assertThrows(IllegalArgumentException.class, () -> generateArtifact());
    }

    @Test
    public void testUnreadableMappingsConfiguration() {
        System.setProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE,
                new ArtifactTestUtils().getResourcePath(Resources.EMPTY_TOSCA_MAPPING_CONFIG));

        GenerationData data = generateArtifact();

        assertSingleError(data);
    }

    @Test
    public void testUnparseableCsarContent() {
        new ArtifactTestUtils().setGeneratorSystemProperties();

        GenerationData data = new AaiArtifactGenerator().generateArtifact(
                "this is not a CSAR archive".getBytes(StandardCharsets.UTF_8), Collections.emptyList(),
                Map.of(AdditionalParams.SERVICE_VERSION.getName(), "1.0"));

        assertSingleError(data);
    }

    @Test
    public void testMissingServiceVersion() throws IOException, InvalidArchiveException {
        new ArtifactTestUtils().setGeneratorSystemProperties();
        byte[] csar = CsarTest.VNF_VENDOR_CSAR.getContent();

        assertThrows(IllegalArgumentException.class, () -> new AaiArtifactGenerator().generateArtifact(csar,
                Collections.emptyList(), Collections.emptyMap()));
    }

    @Test
    public void testInvalidServiceVersion() throws IOException, InvalidArchiveException {
        new ArtifactTestUtils().setGeneratorSystemProperties();
        byte[] csar = CsarTest.VNF_VENDOR_CSAR.getContent();
        Map<String, String> params = Map.of(AdditionalParams.SERVICE_VERSION.getName(), "not-a-version");

        assertThrows(IllegalArgumentException.class,
                () -> new AaiArtifactGenerator().generateArtifact(csar, Collections.emptyList(), params));
    }

    private void assertSingleError(GenerationData data) {
        assertThat(data.getResultData().size(), is(0));
        assertThat(data.getErrorData().keySet(), hasItem(ArtifactType.AAI.name()));
        assertThat(data.getErrorData().get(ArtifactType.AAI.name()).size(), is(1));
    }

    private GenerationData generateArtifact() {
        return new AaiArtifactGenerator().generateArtifact(new byte[0], Collections.emptyList(),
                Collections.emptyMap());
    }
}
