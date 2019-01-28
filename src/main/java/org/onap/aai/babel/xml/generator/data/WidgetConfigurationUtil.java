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
import java.util.Map.Entry;
import java.util.Properties;
import org.onap.aai.babel.xml.generator.model.Model;

public class WidgetConfigurationUtil {

    private static Properties config;
    private static List<String> instanceGroups = Collections.emptyList();
    private static Map<String, Class<? extends Model>> typeToModel = new HashMap<>();

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

    /**
     * Create the mappings from TOSCA type to Widget type. The Properties store a set of TOSCA type prefix Strings.
     * These keys take a single class name (String), which is used to map to a Widget Class in the Model.
     * 
     * @param map
     *            the key/value pairs of TOSCA type and Class name
     */
    @SuppressWarnings("unchecked")
    public static void setTypeMappings(Map<String, String> map) {
        for (Entry<String, String> entry : map.entrySet()) {
            final String toscaType = entry.getKey();
            final String javaBean = entry.getValue();
            final String modelClassName = Model.class.getPackage().getName() + "." + javaBean;
            try {
                typeToModel.put(toscaType, (Class<? extends Model>) Class.forName(modelClassName));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        String.format("Unsupported type \"%s\" for TOSCA mapping %s: no class found for %s", //
                                javaBean, toscaType, modelClassName));
            }
        }
    }

    public static Class<? extends Model> getModelFromType(String typePrefix) {
        return typeToModel.get(typePrefix);
    }
}
