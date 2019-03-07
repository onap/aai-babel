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

import com.google.common.base.Enums;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Widget;
import org.onap.aai.babel.xml.generator.model.Widget.Type;
import org.onap.aai.babel.xml.generator.types.ModelType;

public class WidgetConfigurationUtil {

    private static Properties config;
    private static List<String> instanceGroups = Collections.emptyList();
    private static Map<String, Resource> typeToResource = new HashMap<>();
    private static Map<String, Widget> typeToWidget = new HashMap<>();

    /*
     * Private constructor to prevent instantiation.
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
        return Optional.ofNullable(typeToResource.get(typePrefix));
    }

    public static Widget createWidgetFromType(Type type) throws XmlArtifactGenerationException {
        Optional<Widget> widget = Optional.ofNullable(typeToWidget.get(type.toString()));
        if (widget.isPresent()) {
            // Make a copy of the Widget found in the mappings table.
            return new Widget(widget.get());
        }
        return null;
    }

    public static void setWidgetTypes(List<WidgetType> types) {
        for (WidgetType type : types) {
            if (type.type == null || type.name == null) {
                throw new IllegalArgumentException("Incomplete widget type specified: " + type);
            }
            Type widgetType = Widget.Type.valueOf(type.type);
            Widget widget = new Widget(widgetType, type.name, type.deleteFlag);
            typeToWidget.put(type.type, widget);
        }
    }

    public static void setWidgetMappings(List<WidgetMapping> mappings) throws IOException {
        for (WidgetMapping mapping : mappings) {
            ModelType modelType = Optional.ofNullable(mapping.type).map(String::toUpperCase)
                    .map(s -> Enums.getIfPresent(ModelType.class, s).orNull()).orElse(null);
            if (mapping.prefix == null || mapping.widget == null || modelType == null) {
                throw new IOException("Invalid widget mapping specified: " + mapping);
            }
            Resource resource = new Resource(Widget.Type.valueOf(mapping.widget), mapping.deleteFlag);
            resource.setModelType(modelType);
            typeToResource.put(mapping.prefix, resource);
        }
    }
}
