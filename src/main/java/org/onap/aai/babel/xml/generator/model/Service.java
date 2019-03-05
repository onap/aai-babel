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

import java.util.Collections;
import java.util.Map;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

public class Service extends Model {

    @Override
    public boolean addResource(Resource resource) {
        return resources.add(resource);
    }

    @Override
    public boolean addWidget(Widget widget) {
        return widgets.add(widget);
    }

    @Override
    public Widget.Type getWidgetType() {
        return Type.SERVICE;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isResource() {
        return false;
    }
}
