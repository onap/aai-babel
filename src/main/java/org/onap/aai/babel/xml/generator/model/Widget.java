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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.error.IllegalAccessException;
import org.onap.aai.babel.xml.generator.types.ModelType;

public class Widget extends Model {

    public static final String GENERATOR_AAI_CONFIGLPROP_NOT_FOUND = "Cannot generate artifacts. Widget configuration not found for %s";

    private Set<String> keys = new HashSet<>();

    protected String name;
    protected WidgetType type;
    protected boolean deleteFlag = false;

    private String modelInvariantId;
    private String modelVersionId;

    public Widget(WidgetType widgetType, String name, boolean deleteFlag, String modelInvariantId, String modelVersionId) {
        type = widgetType;
        this.name = name;
        this.deleteFlag = deleteFlag;
        this.modelInvariantId = modelInvariantId;
        this.modelVersionId = modelVersionId;
    }

    /**
     * Copy Constructor.
     *
     * @param baseWidget
     * @throws XmlArtifactGenerationException
     *             if there is no widget mapping defined for any of the VSERVER child types
     */
    public Widget(Widget baseWidget) throws XmlArtifactGenerationException {
        this(baseWidget.getWidgetType(), baseWidget.getName(), baseWidget.getDeleteFlag(), baseWidget.getWidgetId(), baseWidget.getId());
        if (this.hasWidgetType("VSERVER")) {
            widgets.add(createWidget("FLAVOR"));
            widgets.add(createWidget("IMAGE"));
            widgets.add(createWidget("TENANT"));
            widgets.add(createWidget("VFC"));
        }
    }

    /**
     * Creates a new widget of the specified type.
     *
     * @param type
     *            String value of the Widget Type
     * @return a new widget of the specified type
     * @throws XmlArtifactGenerationException
     *             if the configured widget mappings do not support the specified type
     */
    public static Widget createWidget(String type) throws XmlArtifactGenerationException {
        Widget widget = WidgetConfigurationUtil.createWidgetFromType(type);
        if (widget == null) {
            throw new XmlArtifactGenerationException("No widget type is defined for " + type);
        }
        return widget;
    }

    /**
     * Creates a new widget of the specified type.
     *
     * @param type
     *            the Widget Type
     * @return a new widget of the specified type
     * @throws XmlArtifactGenerationException
     *             if there is no configuration defined for the specified type
     */
    public static Widget createWidget(WidgetType type) throws XmlArtifactGenerationException {
        return createWidget(type.toString());
    }

    public String getId() {
        return modelVersionId;
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
        return modelInvariantId;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public WidgetType getWidgetType() {
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
        if (getWidgetType() == WidgetType.valueOf("VSERVER")) {
            return widgets.add(widget);
        }
        return true;
    }

    @Override
    public String toString() {
        return getName() + " Widget keys=" + keys + ", resources=" + resources + ", widgets=" + widgets;
    }

    @Override
    public boolean getDeleteFlag() {
        return deleteFlag;
    }

    @Override
    public String getModelTypeName() {
        throw new IllegalAccessException(GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION);
    }

    @Override
    public String getModelId() {
        throw new IllegalAccessException(GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION);
    }

    @Override
    public String getModelNameVersionId() {
        throw new IllegalAccessException(GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION);
    }

}
