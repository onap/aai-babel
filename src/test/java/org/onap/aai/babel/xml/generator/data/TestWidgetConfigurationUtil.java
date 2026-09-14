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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.xml.generator.model.WidgetType;

/**
 * Tests {@link WidgetConfigurationUtil}.
 */
public class TestWidgetConfigurationUtil {

    @AfterEach
    public void resetStaticState() {
        WidgetConfigurationUtil.setSupportedInstanceGroups(Collections.emptyList());
        WidgetType.clearElements();
    }

    @Test
    public void testPrivateConstructor() throws ReflectiveOperationException {
        Constructor<WidgetConfigurationUtil> constructor = WidgetConfigurationUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException e = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
    }

    @Test
    public void testWidgetTypeMissingType() {
        assertThrows(IllegalArgumentException.class,
                () -> WidgetConfigurationUtil.setWidgetTypes(List.of(createWidgetTypeConfig(null, "name", "1", "2"))));
    }

    @Test
    public void testWidgetTypeMissingName() {
        assertThrows(IllegalArgumentException.class,
                () -> WidgetConfigurationUtil.setWidgetTypes(List.of(createWidgetTypeConfig("SERVICE", null, "1", "2"))));
    }

    @Test
    public void testWidgetTypeMissingModelInvariantId() {
        assertThrows(IllegalArgumentException.class, () -> WidgetConfigurationUtil
                .setWidgetTypes(List.of(createWidgetTypeConfig("SERVICE", "name", null, "2"))));
    }

    @Test
    public void testWidgetTypeMissingModelVersionId() {
        assertThrows(IllegalArgumentException.class, () -> WidgetConfigurationUtil
                .setWidgetTypes(List.of(createWidgetTypeConfig("SERVICE", "name", "1", null))));
    }

    @Test
    public void testWidgetMappingMissingPrefix() {
        assertThrows(IOException.class,
                () -> WidgetConfigurationUtil.setWidgetMappings(List.of(createWidgetMapping(null, "resource", "VF"))));
    }

    @Test
    public void testWidgetMappingMissingWidget() {
        assertThrows(IOException.class, () -> WidgetConfigurationUtil
                .setWidgetMappings(List.of(createWidgetMapping("org.openecomp.resource", "resource", null))));
    }

    @Test
    public void testWidgetMappingInvalidModelType() {
        assertThrows(IOException.class, () -> WidgetConfigurationUtil
                .setWidgetMappings(List.of(createWidgetMapping("org.openecomp.resource", "not-a-model-type", "VF"))));
    }

    @Test
    public void testWidgetMappingNullModelType() {
        assertThrows(IOException.class, () -> WidgetConfigurationUtil
                .setWidgetMappings(List.of(createWidgetMapping("org.openecomp.resource", null, "VF"))));
    }

    @Test
    public void testSupportedInstanceGroups() {
        WidgetConfigurationUtil.setSupportedInstanceGroups(List.of("org.openecomp.groups.NetworkCollection"));
        assertThat(WidgetConfigurationUtil.isSupportedInstanceGroup("org.openecomp.groups.NetworkCollection"), is(true));
        assertThat(WidgetConfigurationUtil.isSupportedInstanceGroup("org.openecomp.groups.UnsupportedGroup"),
                is(false));
    }

    private WidgetTypeConfig createWidgetTypeConfig(String type, String name, String modelInvariantId,
            String modelVersionId) {
        WidgetTypeConfig config = new WidgetTypeConfig();
        config.type = type;
        config.name = name;
        config.modelInvariantId = modelInvariantId;
        config.modelVersionId = modelVersionId;
        return config;
    }

    private WidgetMapping createWidgetMapping(String prefix, String type, String widget) {
        WidgetMapping mapping = new WidgetMapping();
        mapping.prefix = prefix;
        mapping.setType(type);
        mapping.setWidget(widget);
        return mapping;
    }
}
