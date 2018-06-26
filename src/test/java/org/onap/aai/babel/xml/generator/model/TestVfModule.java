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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

/**
 * Direct tests of the Model class VfModule so as to improve code coverage
 */
public class TestVfModule {

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        InputStream in = TestVfModule.class.getClassLoader().getResourceAsStream("artifact-generator.properties");
        Properties properties = new Properties();
        properties.load(in);
        in.close();
        WidgetConfigurationUtil.setConfig(properties);
    }

    @Test
    public void testCreateVfModule() {
        VfModule vf = new VfModule();
        Map<String, String> modelIdentInfo = new HashMap<>();
        modelIdentInfo.put("UUID", "dummy_uuid");
        vf.populateModelIdentificationInformation(modelIdentInfo);
        assertThat(vf.hashCode(), is(notNullValue()));
        assertThat(vf.equals(vf), is(true));
        // Tests that the overridden equals() method correctly returns false for a different type of Object
        // This is necessary to achieve complete code coverage
        assertThat(vf.equals("string"), is(false)); // NOSONAR
    }

    @Test
    public void testNonMemberWidgetToVf() {
        VfModule vf = new VfModule();
        Widget widget = Widget.getWidget(Type.SERVICE);
        vf.setMembers(Collections.singletonList(widget.getId()));
        vf.addWidget(widget);
    }

    @Test
    public void testAddServiceWidgetToVf() {
        VfModule vf = new VfModule();
        addWidgetToModule(vf, Type.SERVICE);
    }

    @Test
    public void testAddVServerWidgetToVf() {
        VfModule vf = new VfModule();
        addWidgetToModule(vf, Type.VSERVER);
    }

    @Test
    public void testAddLIntfWidgetToVf() {
        VfModule vf = new VfModule();
        addWidgetToModule(vf, Type.LINT);
        addWidgetToModule(vf, Type.VSERVER);
        addWidgetToModule(vf, Type.LINT);
    }

    @Test
    public void testAddVolumeWidgetToVf() {
        VfModule vf = new VfModule();
        addWidgetToModule(vf, Type.VOLUME);
        addWidgetToModule(vf, Type.VSERVER);
        addWidgetToModule(vf, Type.VOLUME);
    }

    @Test
    public void testAddOamNetworkWidgetToVf() {
        VfModule vf = new VfModule();
        addWidgetToModule(vf, Type.OAM_NETWORK);
    }

    private void addWidgetToModule(VfModule vfModule, Type widgeType) {
        Widget widget = Widget.getWidget(widgeType);
        String id = widget.getId();
        widget.addKey(id);
        vfModule.setMembers(Collections.singletonList(id));
        vfModule.addWidget(widget);
    }
}
