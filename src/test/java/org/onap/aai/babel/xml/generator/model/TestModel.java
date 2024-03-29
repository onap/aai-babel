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

package org.onap.aai.babel.xml.generator.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;

/**
 * Direct tests of the Model abstract class (to improve code coverage). Not all methods are tested here. Some are
 * covered by the tests of derived classes.
 */
public class TestModel {

    /**
     * Load the Widget mapping configuration.
     *
     * @throws IOException
     *             if the mappings configuration cannot be loaded
     */
    @BeforeAll
    public static void setup() throws IOException {
        new ArtifactTestUtils().loadWidgetMappings();
    }

    @Test
    public void testGetModels() {
        assertThat(Model.getModelFor(null), is(nullValue()));
        assertThat(Model.getModelFor(""), is(nullValue()));
        assertThat(Model.getModelFor("any.unknown.type"), is(nullValue()));

        assertMapping("org.openecomp.resource.vfc", WidgetType.valueOf("VSERVER"));
        assertMapping("org.openecomp.resource.cp", WidgetType.valueOf("LINT"));
        assertMapping("org.openecomp.cp", WidgetType.valueOf("LINT"));
        assertMapping("org.openecomp.cp.some.suffix", WidgetType.valueOf("LINT"));
        assertMapping("org.openecomp.resource.vl", WidgetType.valueOf("L3_NET"));
        assertMapping("org.openecomp.resource.vf", WidgetType.valueOf("VF"));
        assertMapping("org.openecomp.groups.vfmodule", WidgetType.valueOf("VFMODULE"));
        assertMapping("org.openecomp.groups.VfModule", WidgetType.valueOf("VFMODULE"));
        assertMapping("org.openecomp.resource.vfc.nodes.heat.cinder", WidgetType.valueOf("VOLUME"));
        assertMapping("org.openecomp.nodes.PortMirroringConfiguration", "Configuration",
                WidgetType.valueOf("CONFIGURATION"));
        assertMapping("any.string", "Configuration", WidgetType.valueOf("CONFIGURATION"));
        assertMapping("org.openecomp.resource.cr.Kk1806Cr1", "CR", WidgetType.valueOf("CR"));
        assertMapping("any.string", "CR", WidgetType.valueOf("CR"));

        assertMapping("org.openecomp.resource.vfc", "an.unknown.type", WidgetType.valueOf("VSERVER"));
    }

    /**
     * Test that there is no exception if processing a Model that has no metadata properties.
     */
    @Test
    public void testNullIdentProperties() {
        assertDoesNotThrow(() -> {
            createTestModel().populateModelIdentificationInformation(null);
        });
    }

    /**
     * Test that an exception occurs if calling code passes an unsupported Widget Type value to the base implementation
     * of the hasWidgetType() method.
     */
    @Test
    public void testUnknownWidgetType() {
        assertThrows(IllegalArgumentException.class, () -> {
            createTestModel().hasWidgetType(null);
        });
    }

    /**
     * Create any Model with a valid WidgetType, for method testing.
     *
     * @return a valid Model for testing purposes
     */
    private Model createTestModel() {
        return new Resource(WidgetType.valueOf("VSERVER"), false);
    }

    /**
     * Assert that the TOSCA type String is mapped to the expected Widget Type.
     *
     * @param toscaType
     *            the TOSCA type or prefix
     * @param widgetType
     *            the type of Widget expected from the mappings
     */
    private void assertMapping(String toscaType, WidgetType widgetType) {
        assertThat(Model.getModelFor(toscaType).getWidgetType(), is(widgetType));
    }

    /**
     * Assert that the TOSCA metadata type is mapped to the expected Widget Type.
     *
     * @param toscaType
     *            the name (or name prefix) of the TOSCA type
     * @param metadataType
     *            the type specified in the TOSCA metadata
     * @param widgetType
     *            the type of Widget expected from the mappings
     */
    private void assertMapping(String toscaType, String metadataType, WidgetType widgetType) {
        assertThat(Model.getModelFor(toscaType, metadataType).getWidgetType(), is(widgetType));
    }

}
