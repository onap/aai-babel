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
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.parser.ArtifactGeneratorToscaParser;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

/**
 * Direct tests of the Model abstract class (to improve code coverage). Not all methods are tested here. Some are
 * covered by the tests of derived classes.
 */
public class TestModel {

    private Widget widgetModel = new Widget(Type.OAM_NETWORK, "oam-network", true);
    private Model anonymousModel;

    /**
     * Initialize the Artifact Generator with filtering and mapping configuration. Also load the Widget to UUID mappings
     * from the Artifact Generator properties.
     *
     * @throws IOException
     *             if the mappings configuration cannot be loaded
     */
    @Before
    public void setup() throws IOException {
        ArtifactTestUtils utils = new ArtifactTestUtils();
        utils.setGeneratorSystemProperties();

        String configLocation = System.getProperty(ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE);
        if (configLocation == null) {
            throw new IllegalArgumentException(
                    String.format(ArtifactGeneratorToscaParser.GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND,
                            ArtifactGeneratorToscaParser.PROPERTY_TOSCA_MAPPING_FILE));
        }

        ArtifactGeneratorToscaParser.initToscaMappingsConfiguration(configLocation);
        utils.loadWidgetToUuidMappings();

        anonymousModel = new Model() {
            @Override
            public boolean addWidget(Widget resource) {
                return false;
            }

            @Override
            public Type getWidgetType() {
                return null;
            }

            @Override
            public String getModelTypeName() {
                return null;
            }
        };
    }

    @Test
    public void testGetModels() {
        assertThat(Model.getModelFor(null), is(nullValue()));
        assertThat(Model.getModelFor(""), is(nullValue()));
        assertThat(Model.getModelFor("any.unknown.type"), is(nullValue()));

        assertMapping("org.openecomp.resource.vfc", Type.VSERVER);
        assertMapping("org.openecomp.resource.cp", Type.LINT);
        assertMapping("org.openecomp.cp", Type.LINT);
        assertMapping("org.openecomp.cp.some.suffix", Type.LINT);
        assertMapping("org.openecomp.resource.vl", Type.L3_NET);
        assertMapping("org.openecomp.resource.vf", Type.VF);
        assertMapping("org.openecomp.groups.vfmodule", Type.VFMODULE);
        assertMapping("org.openecomp.groups.VfModule", Type.VFMODULE);
        assertMapping("org.openecomp.resource.vfc.nodes.heat.cinder", Type.VOLUME);
        assertMapping("org.openecomp.nodes.PortMirroringConfiguration", "Configuration", Type.CONFIGURATION);
        assertMapping("any.string", "Configuration", Type.CONFIGURATION);
        assertMapping("org.openecomp.resource.cr.Kk1806Cr1", "CR", Type.CR);
        assertMapping("any.string", "CR", Type.CR);

        assertMapping("org.openecomp.resource.vfc", "an.unknown.type", Type.VSERVER);
    }

    /**
     * Assert that the TOSCA type String is mapped to the expected Widget Type.
     * 
     * @param toscaType
     *            the TOSCA type or prefix
     * @param widgetType
     *            the type of Widget expected from the mappings
     */
    private void assertMapping(String toscaType, Type widgetType) {
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
    private void assertMapping(String toscaType, String metadataType, Type widgetType) {
        assertThat(Model.getModelFor(toscaType, metadataType).getWidgetType(), is(widgetType));
    }

    @Test
    public void testGetModelNameVersionId() {
        assertThat(anonymousModel.getModelNameVersionId(), is(nullValue()));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testGetModelNameVersionIdIsUnsupported() {
        assertThat(widgetModel.getModelNameVersionId(), is(nullValue()));
    }

}
