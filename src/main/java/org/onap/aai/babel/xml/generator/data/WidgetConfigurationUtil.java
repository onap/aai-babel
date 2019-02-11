/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 European Software Marketing Ltd.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Widget;

public class WidgetConfigurationUtil {

    private static Properties config;
    private static List<String> instanceGroups = Collections.emptyList();
    private static Map<String, Resource> typeToWidget = new HashMap<>();

    /*
     * Private constructor to prevent instantiation
     */
    private WidgetConfigurationUtil() {
        throw new UnsupportedOperationException("This static class should not be instantiated!");
    }

    public static Properties getConfig() {
        return config;
    }

    public static void setConfig(Properties config) {
        WidgetConfigurationUtil.config = config;
    }

    public static void setSupportedInstanceGroups(List<String> supportedInstanceGroups) {
        instanceGroups = supportedInstanceGroups;
    }

    public static boolean isSupportedInstanceGroup(String groupType) {
        return instanceGroups.contains(groupType);
    }

    public static Optional<Resource> createModelFromType(String typePrefix) {
        return Optional.ofNullable(typeToWidget.get(typePrefix));
    }

    public static void setWidgetMappings(List<WidgetMapping> mappings) {
        for (WidgetMapping mapping : mappings) {
            if (mapping.prefix == null || mapping.widget == null) {
                throw new IllegalArgumentException("Incomplete widget mapping specified: " + mapping);
            }
            Resource resource = new Resource(Widget.Type.valueOf(mapping.widget), mapping.deleteFlag);
            resource.setIsResource(mapping.type.equalsIgnoreCase("resource"));
            typeToWidget.put(mapping.prefix, resource);
        }
    }
}
