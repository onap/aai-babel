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

package org.onap.aai.babel.xml.generator.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.Widget.Type;
import org.onap.aai.babel.xml.generator.types.ModelType;

/**
 * Direct tests of the Model abstract class (to improve code coverage). Not all methods are tested here. Some are
 * covered by the tests of derived classes.
 */
public class TestModel {

    private Service serviceModel = new Service();
    private List<Resource> resourceModels = Arrays.asList(new VirtualFunction(), new InstanceGroup());
    private Widget widgetModel = new OamNetwork();
    private Model anonymousModel;

    static {
        System.setProperty("APP_HOME", ".");
    }

    /**
     * Load the Widget to UUID mappings from the Artifact Generator properties.
     *
     * @throws FileNotFoundException
     *             if the properties file is missing
     * @throws IOException
     *             if the properties file is not loaded
     */
    @Before
    public void setup() throws FileNotFoundException, IOException {
        InputStream in = TestModel.class.getClassLoader().getResourceAsStream("artifact-generator.properties");
        Properties properties = new Properties();
        properties.load(in);
        in.close();
        WidgetConfigurationUtil.setConfig(properties);

        anonymousModel = new Model() {
            @Override
            public boolean addResource(Resource resource) {
                return false;
            }

            @Override
            public boolean addWidget(Widget resource) {
                return false;
            }

            @Override
            public Type getWidgetType() {
                return null;
            }
        };
    }

    @Test
    public void testGetModels() {
        assertThat(Model.getModelFor(null), is(nullValue()));
        assertThat(Model.getModelFor(""), is(nullValue()));
        assertThat(Model.getModelFor("any.unknown.type"), is(nullValue()));

        assertThat(Model.getModelFor("org.openecomp.resource.vf.allottedResource"), instanceOf(AllotedResource.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vf.allottedResource.with.sub.type"),
                instanceOf(AllotedResource.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vfc.AllottedResource"),
                instanceOf(ProvidingService.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vfc"), instanceOf(VServerWidget.class));
        assertThat(Model.getModelFor("org.openecomp.resource.cp"), instanceOf(LIntfWidget.class));
        assertThat(Model.getModelFor("org.openecomp.cp"), instanceOf(LIntfWidget.class));
        assertThat(Model.getModelFor("org.openecomp.cp.some.suffix"), instanceOf(LIntfWidget.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vl"), instanceOf(L3Network.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vf"), instanceOf(VirtualFunction.class));
        assertThat(Model.getModelFor("org.openecomp.groups.vfmodule"), instanceOf(VfModule.class));
        assertThat(Model.getModelFor("org.openecomp.groups.VfModule"), instanceOf(VfModule.class));
        assertThat(Model.getModelFor("org.openecomp.resource.vfc.nodes.heat.cinder"), instanceOf(VolumeWidget.class));
        assertThat(Model.getModelFor("org.openecomp.nodes.PortMirroringConfiguration"),
                instanceOf(Configuration.class));
        assertThat(Model.getModelFor("org.openecomp.nodes.PortMirroringConfiguration", "Configuration"),
                instanceOf(Configuration.class));
        assertThat(Model.getModelFor("any.string", "Configuration"), instanceOf(Configuration.class));
        assertThat(Model.getModelFor("org.openecomp.resource.cr.Kk1806Cr1", "CR"), instanceOf(CR.class));
        assertThat(Model.getModelFor("any.string", "CR"), instanceOf(CR.class));

        assertThat(Model.getModelFor("org.openecomp.resource.vfc", "an.unknown.type"), instanceOf(VServerWidget.class));
    }

    @Test
    public void testGetCardinality() {
        resourceModels.get(0).getCardinality();
    }

    @Test
    public void testGetModelType() {
        assertThat(serviceModel.getModelType(), is(ModelType.SERVICE));
        for (Resource resourceModel : resourceModels) {
            assertThat(resourceModel.getModelType(), is(ModelType.RESOURCE));
        }
        assertThat(widgetModel.getModelType(), is(ModelType.WIDGET));
        assertThat(anonymousModel.getModelType(), is(nullValue()));
    }

    @Test
    public void testGetModelNameVersionId() {
        assertThat(anonymousModel.getModelNameVersionId(), is(nullValue()));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testGetModelNameVersionIdIsUnsupported() {
        assertThat(widgetModel.getModelNameVersionId(), is(nullValue()));
        assertThat(resourceModels.get(0).getModelType(), is(ModelType.RESOURCE));
        assertThat(widgetModel.getModelType(), is(ModelType.WIDGET));
        assertThat(anonymousModel.getModelType(), is(nullValue()));
    }

}
