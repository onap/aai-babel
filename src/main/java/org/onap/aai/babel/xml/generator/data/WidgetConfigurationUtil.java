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
package org.onap.aai.babel.xml.generator.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class WidgetConfigurationUtil {

    private static Properties config;
    private static List<String> instanceGroups = Collections.emptyList();

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

    public static void setFilterConfig(Properties properties) {
        String instanceGroupsList = (String) properties.get("AAI.instance-group-types");
        if (instanceGroupsList != null) {
            instanceGroups = Arrays.asList(instanceGroupsList.split(","));
        }
    }

    public static boolean isSupportedInstanceGroup(String groupType) {
        return instanceGroups.contains(groupType);
    }
}
