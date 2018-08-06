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

public class Resource extends Model {

    @Override
    public int hashCode() {
        return getModelNameVersionId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return getModelNameVersionId().equals(((Resource) obj).getModelNameVersionId());
        }
        return false;
    }

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
        org.onap.aai.babel.xml.generator.types.Model model =
                this.getClass().getAnnotation(org.onap.aai.babel.xml.generator.types.Model.class);
        return model.widget();
    }
}
