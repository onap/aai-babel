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

package org.onap.aai.babel.testdata;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import org.onap.aai.babel.csar.extractor.InvalidArchiveException;
import org.onap.aai.babel.csar.extractor.YamlExtractor;
import org.onap.aai.babel.csar.vnfcatalog.ToscaToCatalogException;
import org.onap.aai.babel.csar.vnfcatalog.VnfVendorImageExtractor;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.data.Artifact;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;

public enum CsarTest {
    // @formatter:off
    VNF_VENDOR_CSAR("catalog_csar.csar"),
    PNF_VENDOR_CSAR("service-Testpnfsvc-csar.csar"),
    NO_VNF_CONFIG_CSAR("noVnfConfiguration.csar"),
    SD_WAN_CSAR_FILE("service-SdWanServiceTest-csar.csar"),
    COS_AVPN_CSAR_FILE("service_CosAvpn_csar.csar"),
    MISSING_METADATA_CSAR("service-MissingMetadataTest.csar"),
    NO_YAML_FILES("noYmlFilesArchive.zip"),
    PORT_MIRROR_CSAR("service_PortMirror.csar"),
    MULTIPLE_VNF_CSAR("catalog_csar_too_many_vnfConfigurations.csar"),
    NETWORK_COLLECTION_CSAR_FILE("service_NetworkCollection.csar"),
    RG_COLLECTOR_615_CSAR_FILE("service-RgCollector615-csar.csar"),
    VNFOD_SERVICE("service-Dev2devnfodservice17July-csar.csar"),
    CHILD_RESOURCE_CSAR_FILE("service-NetworkCloudVnfServiceMock-csar.csar"),
    SERVICE_PROXY_CSAR_FILE("service-S1-csar.csar");

    // @formatter:on

    private String filename;
    private ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();

    CsarTest(String filename) {
        this.filename = filename;
    }

    public String getName() {
        return filename;
    }

    public byte[] getContent() throws IOException {
        return artifactTestUtils.getCompressedArtifact(filename);
    }

    /**
     * Extract YAML Artifacts.
     *
     * @return the extracted artifacts
     * @throws InvalidArchiveException
     *             if the CSAR is invalid
     * @throws IOException
     *             for I/O errors
     */
    public List<Artifact> extractArtifacts() throws InvalidArchiveException, IOException {
        return new YamlExtractor().extract(getContent(), getName(), "v1");
    }

    /**
     * Extract VNF Vendor Image Artifacts.
     *
     * @return the extracted artifacts
     * @throws ToscaToCatalogException
     *             if the CSAR content is not valid
     * @throws IOException
     *             if an I/O exception occursSince:
     */
    public BabelArtifact extractVnfVendorImages() throws ToscaToCatalogException, IOException {
        return new VnfVendorImageExtractor().extract(getContent());
    }

    /**
     * Create a BabelRequest containing the encoded CSAR content.
     *
     * @return a new Babel request for this CSAR
     * @throws IOException
     *             if an I/O exception occurs
     */
    public String getJsonRequest() throws IOException {
        return new Gson().toJson(getBabelRequest());
    }

    public BabelRequest getBabelRequest() throws IOException {
        BabelRequest request = new BabelRequest();
        request.setArtifactName(getName());
        request.setArtifactVersion("1.0");
        request.setCsar(new String(GeneratorUtil.encode(getContent())));
        return request;
    }

    /**
     * Create a BabelRequest containing the encoded CSAR content by passing in the artifact version.
     *
     * @return a new Babel request for this CSAR
     * @throws IOException
     *             if an I/O exception occurs
     */
    public String getJsonRequestWithArtifactVersion(String artifactVersion) throws IOException {
        BabelRequest babelRequest = getBabelRequestWithArtifactVersion(artifactVersion);
        return new Gson().toJson(babelRequest);
    }
    public BabelRequest getBabelRequestWithArtifactVersion(String artifactVersion) throws IOException {
        BabelRequest request = new BabelRequest();
        request.setArtifactName(getName());
        request.setArtifactVersion(artifactVersion);
        request.setCsar(new String(GeneratorUtil.encode(getContent())));
        return request;
    }
}
