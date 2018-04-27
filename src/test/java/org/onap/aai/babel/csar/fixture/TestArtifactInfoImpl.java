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

import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.onap.sdc.api.notification.IArtifactInfo;

/**
 * This class is an implementation of IArtifactInfo for test purposes.
 */
public class TestArtifactInfoImpl implements IArtifactInfo {

    private String artifactName;
    private String artifactType;
    private String artifactDescription;
    private String artifactVersion;

    @Override
    public String getArtifactName() {
        return artifactName;
    }

    void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @Override
    public String getArtifactType() {
        return artifactType;
    }

    void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    @Override
    public String getArtifactURL() {
        return null;
    }

    @Override
    public String getArtifactChecksum() {
        return null;
    }

    @Override
    public String getArtifactDescription() {
        return artifactDescription;
    }

    void setArtifactDescription(String artifactDescription) {
        this.artifactDescription = artifactDescription;
    }

    @Override
    public Integer getArtifactTimeout() {
        return null;
    }

    @Override
    public String getArtifactVersion() {
        return artifactVersion;
    }

    void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Override
    public String getArtifactUUID() {
        return null;
    }

    @Override
    public IArtifactInfo getGeneratedArtifact() {
        return null;
    }

    @Override
    public java.util.List<IArtifactInfo> getRelatedArtifacts() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestArtifactInfoImpl)) {
            return false;
        } else if (obj == this) {
            return true;
        }
        TestArtifactInfoImpl rhs = (TestArtifactInfoImpl) obj;
     // @formatter:off
     return new EqualsBuilder()
                  .append(artifactType, rhs.artifactType)
                  .append(artifactDescription, rhs.artifactDescription)
                  .append(artifactVersion, rhs.artifactVersion)
                  .isEquals();
     // @formatter:on
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.artifactType, this.artifactDescription, this.artifactVersion);
    }
}
