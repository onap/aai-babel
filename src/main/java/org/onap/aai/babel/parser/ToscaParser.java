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

package org.onap.aai.babel.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.enums.SdcTypes;
import org.onap.sdc.tosca.parser.impl.SdcPropertyNames;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;

/**
 * Helper class to isolate the deprecated tosca parser API methods.
 *
 */
public class ToscaParser {

    private ToscaParser() {
        // Not to be instantiated in this version
    }

    @SuppressWarnings("deprecation")
    public static Stream<NodeTemplate> getServiceNodeTemplates(ISdcCsarHelper csarHelper) {
        return csarHelper.getServiceNodeTemplates().stream(); // NOSONAR
    }

    @SuppressWarnings("deprecation")
    public static List<Group> getServiceLevelGroups(ISdcCsarHelper csarHelper) {
        return Optional.ofNullable(csarHelper.getGroupsOfTopologyTemplate()).orElse(new ArrayList<>()); // NOSONAR
    }

    /**
     * Create a Predicate that will return true if and only if the supplied NodeTemplate's metadata contains a
     * <code>type</code> value matching the specified SDC Type.
     *
     * @param sdcType
     *            the type metadata value to match against
     * @return a Predicate for matching the NodeTemplate to the SDC Type
     */
    public static Predicate<NodeTemplate> filterOnType(SdcTypes sdcType) {
        return nodeTemplate -> (nodeTemplate.getMetaData() != null
                && sdcType.getValue().equals(nodeTemplate.getMetaData().getValue(SdcPropertyNames.PROPERTY_NAME_TYPE)));
    }

}
