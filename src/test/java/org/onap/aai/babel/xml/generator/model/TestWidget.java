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
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.types.ModelType;

/**
 * Direct tests of the Widget class for code coverage.
 */
public class TestWidget {

    /**
     * Load the Widget mappings configuration.
     *
     * @throws IOException
     *             if the mappings configuration cannot be loaded
     */
    @BeforeClass
    public static void setup() throws IOException {
        new ArtifactTestUtils().loadWidgetMappings();
    }

    @Test
    public void testGetWidgets() throws XmlArtifactGenerationException {
        Widget widget = Widget.createWidget("SERVICE");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("service-instance"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("VF");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("generic-vnf"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("VFC");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vnfc"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("VSERVER");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vserver"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("VOLUME");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("volume"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("FLAVOR");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("flavor"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.createWidget("TENANT");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("tenant"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.createWidget("VOLUME_GROUP");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("volume-group"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("LINT");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("l-interface"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("L3_NET");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("l3-network"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("VFMODULE");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("vf-module"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("IMAGE");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("image"));
        assertThat(widget.getDeleteFlag(), is(false));

        widget = Widget.createWidget("OAM_NETWORK");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("oam-network"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("ALLOTTED_RESOURCE");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("allotted-resource"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("TUNNEL_XCONNECT");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("tunnel-xconnect"));
        assertThat(widget.getDeleteFlag(), is(true));

        widget = Widget.createWidget("CONFIGURATION");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getName(), is("configuration"));
        assertThat(widget.getDeleteFlag(), is(true));
    }

    @Test
    public void testWidgetMethods() throws XmlArtifactGenerationException {
        Widget widget = Widget.createWidget("SERVICE");
        assertThat(widget.getType(), is(ModelType.WIDGET));
        assertThat(widget.getWidgetId(), is("service-instance-invariant-id"));
        assertThat(widget.addWidget(Widget.createWidget("TENANT")), is(true));
        assertThat(widget.memberOf(null), is(false));
        assertThat(widget.memberOf(Collections.emptyList()), is(false));
    }

    /**
     * Call equals() method for code coverage.
     *
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for the test Widget Type
     */
    @Test
    public void testEquals() throws XmlArtifactGenerationException {
        Widget widgetModel = Widget.createWidget("OAM_NETWORK");

        // equals() is reflexive
        assertThat(widgetModel.equals(widgetModel), is(true));

        // equals() is symmetric
        Widget widgetModelB = Widget.createWidget("OAM_NETWORK");
        assertThat(widgetModel.equals(widgetModelB), is(true));
        assertThat(widgetModelB.equals(widgetModel), is(true));

        assertThat(widgetModel.equals(null), is(false));
        assertThat(widgetModel.equals(Widget.createWidget("VSERVER")), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnknownWidget() throws XmlArtifactGenerationException {
        WidgetType.valueOf("invalid-widget-name");
    }

    /**
     * Try to get the Widget object for an unsupported (non-configured) type.
     *
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for the specified Widget type
     */
    @Test(expected = XmlArtifactGenerationException.class)
    public void testGetDynamicWidget() throws XmlArtifactGenerationException {
        Widget.createWidget(new WidgetType(null));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testAddResourceIsUnsupported() throws XmlArtifactGenerationException {
        Widget.createWidget("OAM_NETWORK").addResource(null);
    }

    // Call Widget methods which are not supported, purely for code coverage.

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testGetModelNameVersionIdIsUnsupported() throws XmlArtifactGenerationException {
        Widget widgetModel = Widget.createWidget("OAM_NETWORK");
        assertThat(widgetModel.getModelNameVersionId(), is(nullValue()));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testGetModelTypeNameIsUnsupported() throws XmlArtifactGenerationException {
        Widget widgetModel = Widget.createWidget("OAM_NETWORK");
        assertThat(widgetModel.getModelTypeName(), is(nullValue()));
    }

    @Test(expected = org.onap.aai.babel.xml.generator.error.IllegalAccessException.class)
    public void testGetModelIdIsUnsupported() throws XmlArtifactGenerationException {
        Widget widgetModel = Widget.createWidget("OAM_NETWORK");
        assertThat(widgetModel.getModelId(), is(nullValue()));
    }

}
