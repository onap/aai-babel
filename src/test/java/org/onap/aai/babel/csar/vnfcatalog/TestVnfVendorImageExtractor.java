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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.babel.testdata.CsarTest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.elements.Metadata;

/**
 * Tests {@link VnfVendorImageExtractor}.
 */
public class TestVnfVendorImageExtractor {

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
        CsarTest.NO_YAML_FILES.extractVnfVendorImages();
    }

    @Test(expected = ToscaToCatalogException.class)
    public void createVendorImageMappingsInvalidFile() throws IOException, ToscaToCatalogException {
        new VnfVendorImageExtractor().extract("not a real file".getBytes());
    }

    @Test
    public void createVendorImageMappingsMoreThanOneVnfConfigurationExists() throws IOException {
        try {
            CsarTest.MULTIPLE_VNF_CSAR.extractArtifacts();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(ToscaToCatalogException.class)));
            assertThat(e.getLocalizedMessage(),
                    is(equalTo("An error occurred trying to get the vnf catalog from a csar file. "
                            + "2 vnfConfigurations were found in the csar file and only one is allowed.")));
        }
    }

    @Test
    public void createVendorImageMappingsNoVnfConfigurationExists() throws IOException, ToscaToCatalogException {
        assertThat(CsarTest.NO_VNF_CONFIG_CSAR.extractVnfVendorImages(), is(nullValue()));
    }

    @Test
    public void createVendorImageMappingsValidFile() throws IOException, ToscaToCatalogException {
        BabelArtifact artifact = CsarTest.VNF_VENDOR_CSAR.extractVnfVendorImages();
        assertThat(artifact.getName(), is(equalTo("vnfVendorImageConfigurations")));
        assertThat(artifact.getType(), is(equalTo(ArtifactType.VNFCATALOG)));
        assertThat(artifact.getPayload(),
                is(equalTo(new ArtifactTestUtils().getRequestJson("vnfVendorImageConfigurations.json"))));
    }

    /**
     * Test that an Exception is created when there are no software versions defined for a VF.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildVendorImageConfigurations() {
        SdcToscaHelper helper = new SdcToscaHelper();
        NodeTemplate vf = helper.addNodeTemplate();
        vf.setMetaData(new Metadata(ImmutableMap.of("resourceVendor", "vendor")));
        vf.setSubMappingToscaTemplate(helper.buildMappings());
        new VnfVendorImageExtractor().buildVendorImageConfigurations(null, vf);
    }

    @Test
    public void testSoftwareVersions() throws ToscaToCatalogException {
        VnfVendorImageExtractor extractor = new VnfVendorImageExtractor();
        SdcToscaHelper helper = new SdcToscaHelper();

        List<String> versions = extractor.extractSoftwareVersions(helper.buildMappings().getNodeTemplates());
        assertThat(versions.size(), is(0));

        helper.addNodeTemplate();
        versions = extractor.extractSoftwareVersions(helper.buildMappings().getNodeTemplates());
        assertThat(versions.size(), is(0));

        helper.addNodeTemplate("string");
        try {
            versions = extractor.extractSoftwareVersions(helper.buildMappings().getNodeTemplates());
            assertThat(versions.size(), is(0));
        } catch (ClassCastException e) {
            assertThat(e.getMessage(), containsString("java.lang.String"));
        }

        HashMap<String, Object> images = new LinkedHashMap<>();
        images.put("image", "string");
        helper.addNodeTemplate(images);
        try {
            versions = extractor.extractSoftwareVersions(helper.buildMappings().getNodeTemplates());
            assertThat(versions.size(), is(1));
        } catch (ClassCastException e) {
            assertThat(e.getMessage(), containsString("java.lang.String"));
        }

        HashMap<String, Object> image = new LinkedHashMap<>();
        image.put("software_version", "1.2.3");
        images.put("image", image);
        helper = new SdcToscaHelper();
        helper.addNodeTemplate(images);
        versions = extractor.extractSoftwareVersions(helper.buildMappings().getNodeTemplates());
        assertThat(versions.size(), is(1));
        assertThat(versions.get(0), is("1.2.3"));
    }
}
