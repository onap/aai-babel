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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
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
        ArtifactTestUtils util = new ArtifactTestUtils();
        util.loadWidgetToUuidMappings();
        util.loadWidgetMappings();
    }

    @Test
    public void testGetWidgets() throws XmlArtifactGenerationException {
        Widget widget = Widget.getWidget(Type.SERVICE);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("service-instance"));
        assertThat(widget.getDeleteFlag(), is(true));
        
        widget = Widget.getWidget(Type.VF);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("generic-vnf"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.getWidget(Type.VFC);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vnfc"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.VSERVER);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vserver"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.VOLUME);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("volume"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.FLAVOR);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("flavor"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.getWidget(Type.TENANT);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("tenant"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.getWidget(Type.VOLUME_GROUP);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("volume-group"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.LINT);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("l-interface"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.L3_NET);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("l3-network"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.VFMODULE);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vf-module"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.IMAGE);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("image"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.getWidget(Type.OAM_NETWORK);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("oam-network"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.ALLOTTED_RESOURCE);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("allotted-resource"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.TUNNEL_XCONNECT);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("tunnel-xconnect"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.getWidget(Type.CONFIGURATION);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("configuration"));
        assertThat(widget.getDeleteFlag(), is(true));
    }

    @Test
    public void testWidgetMethods() throws XmlArtifactGenerationException {
        Widget widget = new Widget(Type.SERVICE, "service-instance", true);
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getWidgetId(), is("service-instance-invariant-id"));
        assertThat(widget.addWidget(Widget.getWidget(Type.TENANT)), is(true));
        assertThat(widget.memberOf(null), is(false));
        assertThat(widget.memberOf(Collections.emptyList()), is(false));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testAddResourceIsUnsupported() throws XmlArtifactGenerationException {
        Widget.getWidget(Type.OAM_NETWORK).addResource(null);
    }
}
