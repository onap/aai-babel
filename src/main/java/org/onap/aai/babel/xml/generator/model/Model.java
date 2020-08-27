/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
 * Copyright (C) 2019-2020 Wipro Limited.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;

public abstract class Model {

    public static final String GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION = "Operation Not Supported for Widgets";

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
        CATEGORY("category") {
        	@Override
            public void populate(Model model, String value) {
                model.category = value;
            }
        },
        DESCRIPTION("serviceDescription", "resourceDescription", "vf_module_description", "description") {
            @Override
            public void populate(Model model, String value) {
                model.modelDescription = value;
            }
        },
        ORCHESTRATION_TYPE("instantiationType"){
            @Override
            public void populate(Model model, String value) {
                model.instantiationType = value;
            }
        },
        NAME_AND_DESCRIPTION("providing_service_name") {
            @Override
            public void populate(Model model, String value) {
                model.modelName = model.modelDescription = value;
            }
        },
        SDNC_MODEL_NAME("sdnc_model_name") {
            @Override
            public void populate(Model model, String value) {
                model.sdncModelName = value;
            }
        },
        SDNC_MODEL_VERSION("sdnc_model_version") {
            @Override
            public void populate(Model model, String value) {
                model.sdncModelVersion = value;
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

    private String modelId; // model-invariant-id
    private String modelName;
    private String modelNameVersionId; // model-version-id
    private String modelVersion;
    private String modelDescription;
    private String instantiationType;
    private String category;
    private String sdncModelVersion;
    private String sdncModelName;
    protected Set<Resource> resources = new HashSet<>();
    protected Set<Widget> widgets = new HashSet<>();

    /**
     * Gets the Resource Model corresponding to the supplied TOSCA type.
     *
     * @param toscaType
     *            the tosca type
     * @return the model for the type, or null
     */
    public static Resource getModelFor(String toscaType) {
        Resource resource = null;
        if (toscaType != null && !toscaType.isEmpty()) {
            resource = getModelFromType(toscaType).orElseGet(() -> Model.getModelFromPrefix(toscaType));
        }
        return resource;
    }

    private static Resource getModelFromPrefix(String toscaType) {
        Resource resource = null;
        int lastSeparator = toscaType.lastIndexOf('.');
        if (lastSeparator != -1) {
            resource = getModelFor(toscaType.substring(0, lastSeparator));
        }
        return resource;
    }

    private static Optional<Resource> getModelFromType(String typePrefix) {
        return WidgetConfigurationUtil.createModelFromType(typePrefix);
    }

    /**
     * Gets the object (model) corresponding to the supplied TOSCA type information, prioritising the metadata
     * information.
     *
     * @param toscaType
     *            the TOSCA type
     * @param metaDataType
     *            the type from the TOSCA metadata
     * @return the model for the type, or null
     */
    public static Resource getModelFor(String toscaType, String metaDataType) {
        if ("Configuration".equals(metaDataType)) {
            return new Resource(WidgetType.valueOf("CONFIGURATION"), true);
        } else if ("CR".equals(metaDataType)) {
            return new Resource(WidgetType.valueOf("CR"), true);
        } else {
            return getModelFor(toscaType);
        }
    }

    public abstract boolean addWidget(Widget resource) throws XmlArtifactGenerationException;

    /**
     * @return the Widget Type of this model.
     */
    public abstract WidgetType getWidgetType();

    public abstract String getModelTypeName();

    /**
     * Check whether the model's Widget Type matches the supplied type.
     *
     * @param type
     *            the Widget Type to compare
     * @return true if the Widget Type of this model matches the supplied type
     */
    public boolean hasWidgetType(String type) {
        return getWidgetType() == WidgetType.valueOf(type);
    }

    public boolean addResource(Resource resource) {
        return resources.add(resource);
    }

    /**
     * Gets delete flag.
     *
     * @return the delete flag
     */
    public boolean getDeleteFlag() {
        return true;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getModelNameVersionId() {
        return modelNameVersionId;
    }

    public String getInstantiationType() {
        return instantiationType;
    }

    public String getCategory() {
    	return category;
    }

    public String getSdncModelName() {
        return sdncModelName;
    }

    public String getSdncModelVersion() {
        return sdncModelVersion;
    }

    /**
     * Gets widget version id.
     *
     * @return the widget version id
     * @throws XmlArtifactGenerationException
     */
    public String getWidgetId() throws XmlArtifactGenerationException {
        return Widget.createWidget(getWidgetType()).getId();
    }

    /**
     * Gets invariant id.
     *
     * @return the invariant id
     * @throws XmlArtifactGenerationException
     */
    public String getWidgetInvariantId() throws XmlArtifactGenerationException {
        return Widget.createWidget(getWidgetType()).getWidgetId();
    }

    /**
     * Populate model identification information.
     *
     * @param modelIdentInfo
     *            the model ident info
     */
    public void populateModelIdentificationInformation(Map<String, String> modelIdentInfo) {
        if (modelIdentInfo == null) {
            return;
        }
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

    @Override
    public String toString() {
        return "Model [type=" + getModelTypeName() + ", name=" + getModelName() + "]";
    }

}
