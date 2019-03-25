/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
 * ===============================================================================
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.babel.util.ArtifactTestUtils;

/**
 * Tests {@link ConfigurationsToBabelArtifactConverter}.
 */
public class ConfigurationsToBabelArtifactConverterTest {
    @Test
    public void testNullListSupplied() {
        assertThat(ConfigurationsToBabelArtifactConverter.convert(null), is(nullValue()));
    }

    @Test
    public void testEmptyListSupplied() {
        assertThat(ConfigurationsToBabelArtifactConverter.convert(new ArrayList<>()), is(nullValue()));
    }

    @Test
    public void testValidListSupplied() throws IOException {
        String expectedJson = new ArtifactTestUtils().getRequestJson("vnfVendorImageConfigurations.json");
        List<VendorImageConfiguration> configurations =
                new Gson().fromJson(expectedJson, new TypeToken<ArrayList<VendorImageConfiguration>>() {}.getType());

        BabelArtifact artifact = ConfigurationsToBabelArtifactConverter.convert(configurations);

        assertThat(artifact.getName(), is(equalTo("vnfVendorImageConfigurations")));
        assertThat(artifact.getType(), is(equalTo(ArtifactType.VNFCATALOG)));
        assertThat(artifact.getPayload(), is(equalTo(expectedJson)));
    }
}
