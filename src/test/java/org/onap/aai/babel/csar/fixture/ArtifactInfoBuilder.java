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
package org.onap.aai.babel.csar.fixture;

import java.util.ArrayList;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;

/**
 * This class builds an instance of IArtifactInfo for test purposes.
 */
public class ArtifactInfoBuilder {

    /**
     * Builds an implementation of IArtifactInfo for test purposes.
     * <p/>
     *
     * @param type type of artifact
     * @param name name of artifact
     * @param description description of artifact
     * @param version version of artifact
     * @return IArtifactInfo implementation of IArtifactInfo from given parameters for test purposes
     */
    public static IArtifactInfo build(final String type, final String name, final String description,
            final String version) {
        IArtifactInfo artifact = new TestArtifactInfoImpl();

        ((TestArtifactInfoImpl) artifact).setArtifactType(type);
        ((TestArtifactInfoImpl) artifact).setArtifactName(name);
        ((TestArtifactInfoImpl) artifact).setArtifactDescription(description);
        ((TestArtifactInfoImpl) artifact).setArtifactVersion(version);

        return artifact;
    }

    /**
     * This method is responsible for building a collection of artifacts from a given set of info.
     * <p/>
     * The info supplied is a two dimensional array with each element of the first dimension representing a single
     * artifact and each element of the second dimension represents a property of the artifact.
     * <p/>
     * The method will call {@link #build(String, String, String, String)} to build each element in the first dimension
     * where the elements of the second dimension are the arguments to {@link #build(String, String, String, String)}.
     * <p/>
     *
     * @param artifactInfoBits a two dimensional array of data used to build the artifacts
     * @return List<IArtifactInfo> a list of artifacts built from the given array of info
     */
    static List<IArtifactInfo> buildArtifacts(final String[][] artifactInfoBits) {
        List<IArtifactInfo> artifacts = new ArrayList<>();

        for (String[] artifactInfoBit : artifactInfoBits) {
            artifacts.add(build(artifactInfoBit[0], artifactInfoBit[1], artifactInfoBit[2], artifactInfoBit[3]));
        }

        return artifacts;
    }
}
