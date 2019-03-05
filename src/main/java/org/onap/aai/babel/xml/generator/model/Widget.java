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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.error.IllegalAccessException;
import org.onap.aai.babel.xml.generator.types.ModelType;

public class Widget extends Model {

    public static final String GENERATOR_AAI_CONFIGLPROP_NOT_FOUND =
            "Cannot generate artifacts. Widget configuration not found for %s";

    public enum Type {
        SERVICE, VF, VFC, VSERVER, VOLUME, FLAVOR, TENANT, VOLUME_GROUP, LINT, L3_NET, VFMODULE, IMAGE, OAM_NETWORK, ALLOTTED_RESOURCE, TUNNEL_XCONNECT, CONFIGURATION, CR, INSTANCE_GROUP;
    }

    private Set<String> keys = new HashSet<>();

    protected String name;
    protected Type type;
    protected boolean deleteFlag = false;

    public Widget(Type widgetType, String name, boolean deleteFlag) {
        type = widgetType;
        this.name = name;
        this.deleteFlag = deleteFlag;
    }

    /**
     * Copy Constructor
     * 
     * @param baseWidget
     * @throws XmlArtifactGenerationException 
     */
    public Widget(Widget baseWidget) throws XmlArtifactGenerationException {
        this(baseWidget.getWidgetType(), baseWidget.getName(), baseWidget.getDeleteFlag());
        if (type == Type.VSERVER) {
            widgets.add(getWidget(Type.FLAVOR));
            widgets.add(getWidget(Type.IMAGE));
            widgets.add(getWidget(Type.TENANT));
            widgets.add(getWidget(Type.VFC));
        }
    }

    /**
     * Gets widget.
     *
     * @param type
     *            the type
     * @return a new widget of the specified type
     * @throws XmlArtifactGenerationException 
     */
    public static Widget getWidget(Type type) throws XmlArtifactGenerationException {
        Widget widget = WidgetConfigurationUtil.createWidgetFromType(type);
        if (widget == null) {
            throw new XmlArtifactGenerationException("No widget type is defined for " + type);
        }
        return widget;
    }

    @Override
    public boolean isResource() {
        return false;
    }

    public String getId() {
        String id = WidgetConfigurationUtil.getConfig()
                .getProperty(ArtifactType.AAI.name() + ".model-version-id." + getName());
        if (id == null) {
            throw new IllegalArgumentException(String.format(GENERATOR_AAI_CONFIGLPROP_NOT_FOUND,
                    ArtifactType.AAI.name() + ".model-version-id." + getName()));
        }
        return id;
    }

    public ModelType getType() {
        return ModelType.WIDGET;
    }

    public String getName() {
        return name;
    }

    /**
     * Get Widget Id from properties file.
     *
     * @return - Widget Id
     */
    @Override
    public String getWidgetId() {
        Properties properties = WidgetConfigurationUtil.getConfig();
        String id = properties.getProperty(ArtifactType.AAI.name() + ".model-invariant-id." + getName());
        if (id == null) {
            throw new IllegalArgumentException(String.format(GENERATOR_AAI_CONFIGLPROP_NOT_FOUND,
                    ArtifactType.AAI.name() + ".model-invariant-id." + getName()));
        }
        return id;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public Type getWidgetType() {
        return type;
    }

    /**
     * Equals method that compares Widget IDs.
     *
     * @param obj
     *            the Widget object to compare
     * @return whether or not obj is equal to this Widget
     */
    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj instanceof Widget) {
            Widget other = (Widget) obj;
            if (getId().equals(other.getId())) {
                other.keys.addAll(this.keys);
                isEqual = true;
            }
        }
        return isEqual;
    }

    public void addKey(String key) {
        this.keys.add(key);
    }

    /**
     * Determine whether one or more keys belonging to this Widget appear in the specified Collection.
     *
     * @param keys
     *            the keys
     * @return the boolean
     */
    public boolean memberOf(Collection<String> keys) {
        if (keys == null) {
            return false;
        }
        return !Collections.disjoint(this.keys, keys);
    }

    @Override
    public boolean addResource(Resource resource) {
        throw new IllegalAccessException(Model.GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION);
    }

    @Override
    public boolean addWidget(Widget widget) {
        if (getWidgetType() == Type.VSERVER) {
            return widgets.add(widget);
        }
        return true;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return getName() + " Widget keys=" + keys + ", resources=" + resources + ", widgets=" + widgets;
    }

    @Override
    public boolean getDeleteFlag() {
        return deleteFlag;
    }
}
