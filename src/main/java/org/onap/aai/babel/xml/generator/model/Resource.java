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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

public class Resource extends Model {

    private Type type;
    private boolean deleteFlag;
    private boolean isResource = true;
    private Map<String, Object> properties = Collections.emptyMap();

    Widget vserver = null;
    boolean addlintf = false;
    boolean addvolume = false;
    List<String> members;

    public Resource(Type type, boolean deleteFlag) {
        this.type = type;
        this.deleteFlag = deleteFlag;
    }

    @Override
    public int hashCode() {
        final String uuid = getModelNameVersionId();
        return uuid == null ? 0 : uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return getModelNameVersionId().equals(((Resource) obj).getModelNameVersionId());
        }
        return false;
    }

    @Override
    public boolean getDeleteFlag() {
        return deleteFlag;
    }

    @Override
    public String getWidgetInvariantId() {
        return Widget.getWidget(getWidgetType()).getWidgetId();
    }

    @Override
    public String getWidgetId() {
        return Widget.getWidget(getWidgetType()).getId();
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setIsResource(boolean isResource) {
        this.isResource = isResource;
    }

    @Override
    public boolean isResource() {
        return isResource;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    @Override
    public boolean addResource(Resource resource) {
        return resources.add(resource);
    }

    /**
     * Adds a Widget.
     *
     * @param widget
     *     the widget
     * @return the boolean
     */
    @Override
    public boolean addWidget(Widget widget) {
        if (type == Type.VFMODULE) {
            if (widget.memberOf(members)) {
                if (vserver == null && widget instanceof VServerWidget) {
                    addVserverWidget(widget);
                } else if (widget instanceof LIntfWidget) {
                    return addLIntfWidget(widget);
                } else if (widget instanceof VolumeWidget) {
                    addVolumeWidget(widget);
                    return true;
                }
                if (!(widget instanceof OamNetwork)) {
                    return widgets.add(widget);
                }
            }
            return false;
        } else {
            return widgets.add(widget);
        }
    }

    public Type getWidgetType() {
        return type;
    }

    @Override
    public String toString() {
        return "Widget type " + getWidgetType() + ", isResource=" + isResource() + ", deleteFlag=" + deleteFlag;
    }

    private void addVolumeWidget(Widget widget) {
        if (vserver != null) {
            vserver.addWidget(widget);
        } else {
            addvolume = true;
        }
    }

    /**
     * @param widget
     * @return
     */
    private boolean addLIntfWidget(Widget widget) {
        if (vserver != null) {
            vserver.addWidget(widget);
            return true;
        } else {
            addlintf = true;
            return false;
        }
    }

    private void addVserverWidget(Widget widget) {
        vserver = widget;
        if (addlintf) {
            vserver.addWidget(new LIntfWidget());
        }
        if (addvolume) {
            vserver.addWidget(new VolumeWidget());
        }
    }

}
