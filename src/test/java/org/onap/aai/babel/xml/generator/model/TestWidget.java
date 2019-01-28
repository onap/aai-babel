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

package org.onap.aai.babel.xml.generator.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.model.Widget.Type;
import org.onap.aai.babel.xml.generator.types.ModelType;

/**
 * Direct tests of the Widget class for code coverage.
 */
public class TestWidget {

    static {
        System.setProperty("APP_HOME", ".");
    }

    /**
     * Load the Widget to UUID mappings from the Artifact Generator properties.
     *
     * @throws IOException
     *             if the properties file is not loaded
     */
    @BeforeClass
    public static void setup() throws IOException {
        new ArtifactTestUtils().loadWidgetToUuidMappings();
    }

    @Test
    public void testGetWidgets() {
        assertThat(Widget.getWidget(Type.SERVICE), instanceOf(ServiceWidget.class));
        assertThat(Widget.getWidget(Type.VF), instanceOf(VfWidget.class));
        assertThat(Widget.getWidget(Type.VFC), instanceOf(VfcWidget.class));
        assertThat(Widget.getWidget(Type.VSERVER), instanceOf(VServerWidget.class));
        assertThat(Widget.getWidget(Type.VOLUME), instanceOf(VolumeWidget.class));
        assertThat(Widget.getWidget(Type.FLAVOR), instanceOf(FlavorWidget.class));
        assertThat(Widget.getWidget(Type.TENANT), instanceOf(TenantWidget.class));
        assertThat(Widget.getWidget(Type.VOLUME_GROUP), instanceOf(VolumeGroupWidget.class));
        assertThat(Widget.getWidget(Type.LINT), instanceOf(LIntfWidget.class));
        assertThat(Widget.getWidget(Type.L3_NET), instanceOf(L3NetworkWidget.class));
        assertThat(Widget.getWidget(Type.VFMODULE), instanceOf(VfModuleWidget.class));
        assertThat(Widget.getWidget(Type.IMAGE), instanceOf(ImageWidget.class));
        assertThat(Widget.getWidget(Type.OAM_NETWORK), instanceOf(OamNetwork.class));
        assertThat(Widget.getWidget(Type.ALLOTTED_RESOURCE), instanceOf(AllotedResourceWidget.class));
        assertThat(Widget.getWidget(Type.TUNNEL_XCONNECT), instanceOf(TunnelXconnectWidget.class));
        assertThat(Widget.getWidget(Type.CONFIGURATION), instanceOf(ConfigurationWidget.class));
    }

    @Test
    public void testWidgetMethods() {
        Widget widget = new ServiceWidget();
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getWidgetId(), is("service-instance-invariant-id"));
        assertThat(widget.addWidget(new TenantWidget()), is(true));
        assertThat(widget.memberOf(null), is(false));
        assertThat(widget.memberOf(Collections.emptyList()), is(false));

        widget = new VolumeGroupWidget(); // just for variety
        assertThat(widget.getWidgetType(), is(nullValue()));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testAddResourceIsUnsupported() {
        new OamNetwork().addResource(null);
    }
}
