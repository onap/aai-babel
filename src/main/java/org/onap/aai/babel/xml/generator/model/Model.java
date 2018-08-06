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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.error.IllegalAccessException;
import org.onap.aai.babel.xml.generator.types.Cardinality;
import org.onap.aai.babel.xml.generator.types.ModelType;
import org.onap.aai.cl.api.Logger;

public abstract class Model {

    public static final String GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION = "Operation Not Supported for Widgets";

    private static Logger log = LogHelper.INSTANCE;

    private static Map<String, Class<? extends Model>> typeToModel = new HashMap<>();
    static {
        typeToModel.put("org.openecomp.resource.vf.allottedResource", AllotedResource.class);
        typeToModel.put("org.openecomp.resource.vfc.AllottedResource", ProvidingService.class);
        typeToModel.put("org.openecomp.resource.vfc", VServerWidget.class);
        typeToModel.put("org.openecomp.resource.cp", LIntfWidget.class);
        typeToModel.put("org.openecomp.cp", LIntfWidget.class);
        typeToModel.put("org.openecomp.resource.vl", L3Network.class);
        typeToModel.put("org.openecomp.resource.vf", VirtualFunction.class);
        typeToModel.put("org.openecomp.groups.vfmodule", VfModule.class);
        typeToModel.put("org.openecomp.groups.VfModule", VfModule.class);
        typeToModel.put("org.openecomp.resource.vfc.nodes.heat.cinder", VolumeWidget.class);
        typeToModel.put("org.openecomp.nodes.PortMirroringConfiguration", Configuration.class);
        typeToModel.put("org.openecomp.resource.cr.Kk1806Cr1", CR.class);
    }

    private enum ModelIdentification {
        ID("vfModuleModelInvariantUUID", "serviceInvariantUUID", "resourceInvariantUUID", "invariantUUID",
                "providing_service_invariant_uuid") {
            @Override
            public void populate(Model model, String value) {
                model.modelId = value;
            }
        },
        NAME_VERSION_ID("vfModuleModelUUID", "resourceUUID", "serviceUUID", "UUID", "providing_service_uuid") {
            @Override
            public void populate(Model model, String value) {
                model.modelNameVersionId = value;
            }
        },
        VERSION("vfModuleModelVersion", "serviceVersion", "resourceversion", "version") {
            @Override
            public void populate(Model model, String value) {
                model.modelVersion = value;
            }
        },
        NAME("vfModuleModelName", "serviceName", "resourceName", "name") {
            @Override
            public void populate(Model model, String value) {
                model.modelName = value;
            }
        },
        DESCRIPTION("serviceDescription", "resourceDescription", "vf_module_description", "description") {
            @Override
            public void populate(Model model, String value) {
                model.modelDescription = value;
            }
        },
        NAME_AND_DESCRIPTION("providing_service_name") {
            @Override
            public void populate(Model model, String value) {
                model.modelName = model.modelDescription = value;
            }
        };

        private static final Map<String, ModelIdentification> propertyToModelIdent;
        private String[] keys;

        ModelIdentification(String... keys) {
            this.keys = keys;
        }

        static {
            Map<String, ModelIdentification> mappings = new HashMap<>();
            for (ModelIdentification ident : ModelIdentification.values()) {
                for (String key : ident.keys) {
                    mappings.put(key, ident);
                }
            }
            propertyToModelIdent = Collections.unmodifiableMap(mappings);
        }

        private static Optional<ModelIdentification> getModelIdentFromProperty(String property) {
            return Optional.ofNullable(propertyToModelIdent.get(property));
        }

        public abstract void populate(Model model, String value);
    }

    private String modelId;
    private String modelName;
    private String modelNameVersionId;
    private String modelVersion;
    private String modelDescription;

    protected Set<Resource> resources = new HashSet<>();
    protected Set<Widget> widgets = new HashSet<>();

    /**
     * Gets the object (model) corresponding to the supplied TOSCA type.
     *
     * @param toscaType
     *            the tosca type
     * @return the model for the type, or null
     */
    public static Model getModelFor(String toscaType) {
        Model model = null;
        if (toscaType != null && !toscaType.isEmpty()) {
            model = getModelFromType(toscaType).orElseGet(() -> Model.getModelFromPrefix(toscaType));
        }
        return model;
    }

    private static Model getModelFromPrefix(String toscaType) {
        Model model = null;
        int lastSeparator = toscaType.lastIndexOf('.');
        if (lastSeparator != -1) {
            model = getModelFor(toscaType.substring(0, lastSeparator));
        }
        return model;
    }

    private static Optional<Model> getModelFromType(String typePrefix) {
        Optional<Model> modelToBeReturned = Optional.empty();
        Class<? extends Model> clazz = typeToModel.get(typePrefix);
        if (clazz != null) {
            try {
                modelToBeReturned = Optional.ofNullable(clazz.getConstructor().newInstance());
            } catch (InstantiationException | java.lang.IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                log.error(ApplicationMsgs.INVALID_CSAR_FILE, e);
            }
        }
        return modelToBeReturned;
    }

    public abstract boolean addResource(Resource resource);

    public abstract boolean addWidget(Widget resource);

    public abstract Widget.Type getWidgetType();

    /**
     * Gets cardinality.
     *
     * @return the cardinality
     */
    public Cardinality getCardinality() {
        org.onap.aai.babel.xml.generator.types.Model model = this.getClass()
                .getAnnotation(org.onap.aai.babel.xml.generator.types.Model.class);
        return model.cardinality();
    }

    /**
     * Gets delete flag.
     *
     * @return the delete flag
     */
    public boolean getDeleteFlag() {
        org.onap.aai.babel.xml.generator.types.Model model = this.getClass()
                .getAnnotation(org.onap.aai.babel.xml.generator.types.Model.class);
        return model.dataDeleteFlag();
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public String getModelId() {
        checkSupported();
        return modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getModelNameVersionId() {
        checkSupported();
        return modelNameVersionId;
    }

    /**
     * Gets model type.
     *
     * @return the model type
     */
    public ModelType getModelType() {
        if (this instanceof Service) {
            return ModelType.SERVICE;
        } else if (this instanceof Resource) {
            return ModelType.RESOURCE;
        } else if (this instanceof Widget) {
            return ModelType.WIDGET;
        } else {
            return null;
        }
    }

    /**
     * Gets widget version id.
     *
     * @return the widget version id
     */
    public String getWidgetId() {
        org.onap.aai.babel.xml.generator.types.Model model = this.getClass()
                .getAnnotation(org.onap.aai.babel.xml.generator.types.Model.class);
        return Widget.getWidget(model.widget()).getId();
    }

    /**
     * Gets invariant id.
     *
     * @return the invariant id
     */
    public String getWidgetInvariantId() {
        org.onap.aai.babel.xml.generator.types.Model model = this.getClass()
                .getAnnotation(org.onap.aai.babel.xml.generator.types.Model.class);
        return Widget.getWidget(model.widget()).getWidgetId();
    }

    /**
     * Populate model identification information.
     *
     * @param modelIdentInfo
     *            the model ident info
     */
    public void populateModelIdentificationInformation(Map<String, String> modelIdentInfo) {
        Iterator<String> iter = modelIdentInfo.keySet().iterator();
        String property;
        while (iter.hasNext()) {
            property = iter.next();
            Optional<ModelIdentification> modelIdent = ModelIdentification.getModelIdentFromProperty(property);
            if (modelIdent.isPresent()) {
                modelIdent.get().populate(this, modelIdentInfo.get(property));
            }
        }
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public Set<Widget> getWidgets() {
        return widgets;
    }

    private void checkSupported() {
        if (this instanceof Widget) {
            throw new IllegalAccessException(GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION);
        }
    }
}
