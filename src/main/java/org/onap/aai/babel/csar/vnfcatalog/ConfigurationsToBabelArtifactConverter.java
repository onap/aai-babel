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
package org.onap.aai.babel.csar.vnfcatalog;

import com.google.gson.Gson;
import java.util.List;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;

/**
 * This class is responsible for converting a collection of VendorImageConfigurations into an instance of a
 * BabelArtifact.
 */
class ConfigurationsToBabelArtifactConverter {
    private ConfigurationsToBabelArtifactConverter() {}

    /**
     * This method converts a collection of VendorImageConfiguration objects into an instance of a BabelArtifact.
     *
     * <p>
     * The method will convert the configurations objects into JSON and this will be stored in the BabelArtifact's
     * payload property.
     *
     * <p>
     * The method will return null if there are no configurations (null or empty) to process.
     *
     * @param configurations collection of VendorImageConfiguration objects into an instance of a BabelArtifact
     * @return BabelArtifact instance representing the configurations or null if there are no configurations.
     */
    static BabelArtifact convert(List<VendorImageConfiguration> configurations) {
        if (configurations != null && !configurations.isEmpty()) {
            String payload = new Gson().toJson(configurations, configurations.getClass());
            return new BabelArtifact("vnfVendorImageConfigurations", ArtifactType.VNFCATALOG, payload);
        } else {
            return null;
        }
    }
}
