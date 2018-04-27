/**
 * ﻿============LICENSE_START=======================================================
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

import org.junit.Test;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

/**
 * Direct tests of the Model so as to improve code coverage
 */
public class TestWidget {

    static {
        if (System.getProperty("APP_HOME") == null) {
            System.setProperty("APP_HOME", ".");
        }
    }

    @Test
    public void testGetWidgets() {
        Widget.getWidget(Type.VFC);
        Widget.getWidget(Type.FLAVOR);
        Widget.getWidget(Type.TENANT);
        Widget.getWidget(Type.VOLUME_GROUP);
        Widget.getWidget(Type.L3_NET);
        Widget.getWidget(Type.IMAGE);
        Widget.getWidget(Type.TUNNEL_XCONNECT);
    }

    @Test
    public void testMethods() {
        new ServiceWidget().addWidget(new TenantWidget());
        new VolumeGroupWidget().getWidgetType();
    }


}
