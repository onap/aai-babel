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

package org.onap.aai.babel.xml.generator.data;

import java.util.List;

public class GroupConfiguration {

    /**
     * Names of Instance Groups that will be processed (not filtered out).
     */
    private List<String> instanceGroupTypes;

    /**
     * Set of Widget Types.
     */
    private List<WidgetTypeConfig> widgetTypes;

    /**
     * Mapping from TOSCA type to Widget directly.
     */
    private List<WidgetMapping> widgetMappings;

    public List<String> getInstanceGroupTypes() {
        return instanceGroupTypes;
    }

    public List<WidgetTypeConfig> getWidgetTypes() {
        return widgetTypes;
    }

    public List<WidgetMapping> getWidgetMappings() {
        return widgetMappings;
    }

}
