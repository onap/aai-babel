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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.types.ModelType;

public class Resource extends Model {

    private WidgetType type;
    private boolean deleteFlag;
    private ModelType modelType = ModelType.RESOURCE;
    private Map<String, Object> properties = Collections.emptyMap();

    Widget vserver = null;
    boolean addlintf = false;
    boolean addvolume = false;
    List<String> members;

    public Resource(WidgetType type, boolean deleteFlag) {
        this.type = type;
        this.deleteFlag = deleteFlag;
    }

    /**
     * Copy Constructor.
     *
     * @param baseResource
     */
    public Resource(Resource baseResource) {
        this(baseResource.getWidgetType(), baseResource.getDeleteFlag());
        setModelType(baseResource.getModelType());
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

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setModelType(ModelType type) {
        this.modelType = type;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    /**
     * Adds a Widget.
     *
     * @param widget
     *            the widget
     * @return the boolean
     * @throws XmlArtifactGenerationException
     */
    @Override
    public boolean addWidget(Widget widget) throws XmlArtifactGenerationException {
        if (type == WidgetType.valueOf("VFMODULE")) {
            if (widget.memberOf(members)) {
                if (vserver == null && widget.hasWidgetType("VSERVER")) {
                    addVserverWidget(widget);
                } else if (widget.hasWidgetType("LINT")) {
                    return addLIntfWidget(widget);
                } else if (widget.hasWidgetType("VOLUME")) {
                    addVolumeWidget(widget);
                    return true;
                }
                if (!widget.hasWidgetType("OAM_NETWORK")) {
                    return widgets.add(widget);
                }
            }
            return false;
        } else {
            return widgets.add(widget);
        }
    }

    @Override
    public WidgetType getWidgetType() {
        return type;
    }

    @Override
    public String getModelTypeName() {
        return "resource";
    }

    @Override
    public String toString() {
        return "Resource [widget type=" + getWidgetType() + ", deleteFlag=" + deleteFlag + ", modelType=" + modelType
                + ", properties=" + properties + ", vserver=" + vserver + ", addlintf=" + addlintf + ", addvolume="
                + addvolume + ", members=" + members + "]";
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

    private void addVserverWidget(Widget widget) throws XmlArtifactGenerationException {
        vserver = widget;
        if (addlintf) {
            vserver.addWidget(Widget.getWidget(WidgetType.valueOf("LINT")));
        }
        if (addvolume) {
            vserver.addWidget(Widget.getWidget(WidgetType.valueOf("VOLUME")));
        }
    }

}
