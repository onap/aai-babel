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

package org.onap.aai.babel.csar.vnfcatalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.babel.util.ArtifactTestUtils;

/**
 * Tests {@link VnfVendorImageExtractor}
 */
public class VnfVendorImageExtractorTest {

    @Test(expected = NullPointerException.class)
    public void createVendorImageMappingsNullCsarSupplied() throws ToscaToCatalogException, IOException {
        new VnfVendorImageExtractor().extract(null);
    }

    @Test(expected = ToscaToCatalogException.class)
    public void createVendorImageMappingsEmptyCsarSupplied() throws ToscaToCatalogException, IOException {
        new VnfVendorImageExtractor().extract(new byte[0]);
    }

    @Test(expected = ToscaToCatalogException.class)
    public void createVendorImageMappingsInvalidCsarFile() throws IOException, ToscaToCatalogException {
        extractArtifact("noYmlFilesArchive.zip");
    }

    @Test(expected = ToscaToCatalogException.class)
    public void createVendorImageMappingsInvalidFile() throws IOException, ToscaToCatalogException {
        extractArtifact("Duff.txt");
    }

    @Test
    public void createVendorImageMappingsMoreThanOneVnfConfigurationExists() throws IOException {
        try {
            extractArtifact("catalog_csar_too_many_vnfConfigurations.csar");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(ToscaToCatalogException.class)));
            assertThat(e.getLocalizedMessage(),
                    is(equalTo("An error occurred trying to get the vnf catalog from a csar file. "
                            + "2 vnfConfigurations were found in the csar file and only one is allowed.")));
        }
    }

    @Test
    public void createVendorImageMappingsNoVnfConfigurationExists() throws IOException, ToscaToCatalogException {
        assertThat(extractArtifact("noVnfConfiguration.csar"), is(nullValue()));
    }

    @Test
    public void createVendorImageMappingsValidFile() throws IOException, ToscaToCatalogException {
        BabelArtifact artifact = extractArtifact("catalog_csar.csar");
        assertThat(artifact.getName(), is(equalTo("vnfVendorImageConfigurations")));
        assertThat(artifact.getType(), is(equalTo(ArtifactType.VNFCATALOG)));
        assertThat(artifact.getPayload(),
                is(equalTo(new ArtifactTestUtils().getRequestJson("vnfVendorImageConfigurations.json"))));
    }

    private BabelArtifact extractArtifact(String artifactName) throws ToscaToCatalogException, IOException {
        return new VnfVendorImageExtractor().extract(new ArtifactTestUtils().getCompressedArtifact(artifactName));
    }
}
