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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;

/**
 * Direct tests of the Model (to improve code coverage).
 */
public class TestModel {

    static {
        if (System.getProperty("AJSC_HOME") == null) {
            System.setProperty("AJSC_HOME", ".");
        }
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        InputStream in = TestModel.class.getClassLoader().getResourceAsStream("artifact-generator.properties");
        Properties properties = new Properties();
        properties.load(in);
        in.close();
        WidgetConfigurationUtil.setConfig(properties);
    }

    @Test
    public void testGetModels() {
        Collection<String> toscaTypes = Arrays.asList("org.openecomp.resource.vf.allottedResource",
                "org.openecomp.resource.cp", "org.openecomp.resource.vfc.nodes.heat.cinder", "any.unknown.type", null);
        for (String toscaType : toscaTypes) {
            Model.getModelFor(toscaType);
        }
    }

    @Test
    public void testGetCardinality() {
        new AllotedResource().getCardinality();
    }

    @Test
    public void testGetModelType() {
        new OamNetwork().getModelType();
    }

}
