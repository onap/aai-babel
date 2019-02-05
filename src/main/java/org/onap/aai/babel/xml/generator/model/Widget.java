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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.data.ArtifactType;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.error.IllegalAccessException;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.aai.babel.xml.generator.types.ModelWidget;
import org.onap.aai.cl.api.Logger;

public abstract class Widget extends Model {

    public static final String GENERATOR_AAI_CONFIGLPROP_NOT_FOUND =
            "Cannot generate artifacts. Widget configuration not found for %s";

    public enum Type {
        SERVICE, VF, VFC, VSERVER, VOLUME, FLAVOR, TENANT, VOLUME_GROUP, LINT, L3_NET, VFMODULE, IMAGE, OAM_NETWORK, ALLOTTED_RESOURCE, TUNNEL_XCONNECT, CONFIGURATION, CR, INSTANCE_GROUP;
    }

    private static Logger log = LogHelper.INSTANCE;

    private Set<String> keys = new HashSet<>();

    private static EnumMap<Widget.Type, Class<? extends Widget>> typeToWidget = new EnumMap<>(Widget.Type.class);
    static {
        typeToWidget.put(Type.SERVICE, ServiceWidget.class);
        typeToWidget.put(Type.VF, VfWidget.class);
        typeToWidget.put(Type.VFC, VfcWidget.class);
        typeToWidget.put(Type.VSERVER, VServerWidget.class);
        typeToWidget.put(Type.VOLUME, VolumeWidget.class);
        typeToWidget.put(Type.FLAVOR, FlavorWidget.class);
        typeToWidget.put(Type.TENANT, TenantWidget.class);
        typeToWidget.put(Type.VOLUME_GROUP, VolumeGroupWidget.class);
        typeToWidget.put(Type.LINT, LIntfWidget.class);
        typeToWidget.put(Type.L3_NET, L3NetworkWidget.class);
        typeToWidget.put(Type.VFMODULE, VfModuleWidget.class);
        typeToWidget.put(Type.IMAGE, ImageWidget.class);
        typeToWidget.put(Type.OAM_NETWORK, OamNetwork.class);
        typeToWidget.put(Type.ALLOTTED_RESOURCE, AllotedResourceWidget.class);
        typeToWidget.put(Type.TUNNEL_XCONNECT, TunnelXconnectWidget.class);
        typeToWidget.put(Type.CONFIGURATION, ConfigurationWidget.class);
        typeToWidget.put(Type.CR, CRWidget.class);
        typeToWidget.put(Type.INSTANCE_GROUP, InstanceGroupWidget.class);
    }

    /**
     * Gets widget.
     *
     * @param type
     *            the type
     * @return the widget
     */
    public static Widget getWidget(Type type) {
        Widget widget = null;
        Class<? extends Widget> clazz = typeToWidget.get(type);
        if (clazz != null) {
            try {
                widget = clazz.getConstructor().newInstance();
            } catch (InstantiationException | java.lang.IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                log.error(ApplicationMsgs.INVALID_CSAR_FILE, e);
            }
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
        ModelWidget widgetModel = this.getClass().getAnnotation(ModelWidget.class);
        return widgetModel.type();
    }

    public String getName() {
        return this.getClass().getAnnotation(ModelWidget.class).name();
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
        return null;
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
        return true;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }
}
