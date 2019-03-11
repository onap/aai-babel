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

package org.onap.aai.babel.xml.generator.api;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.aai.babel.xml.generator.model.Service;
import org.onap.aai.babel.xml.generator.model.Widget;
import org.onap.aai.babel.xml.generator.xsd.ModelElement;
import org.onap.aai.babel.xml.generator.xsd.ModelElements;
import org.onap.aai.babel.xml.generator.xsd.ModelVer;
import org.onap.aai.babel.xml.generator.xsd.ModelVers;
import org.onap.aai.babel.xml.generator.xsd.Relationship;
import org.onap.aai.babel.xml.generator.xsd.RelationshipData;
import org.onap.aai.babel.xml.generator.xsd.RelationshipList;
import org.onap.aai.cl.api.Logger;
import org.w3c.dom.DOMException;

/**
 * Generates the A&AI XML models from the Service/Resource/Widget Java models.
 */
public class AaiModelGenerator {

    private static Logger log = LogHelper.INSTANCE;

    /**
     * Method to generate the AAI model for a Service or Resource.
     *
     * @param model
     *            Java object model representing an AAI {@link Service} or {@link Resource} model
     * @return XML representation of the model in String format
     * @throws XmlArtifactGenerationException
     */
    public String generateModelFor(Model model) throws XmlArtifactGenerationException {
        org.onap.aai.babel.xml.generator.xsd.Model aaiModel = createJaxbModel(model);
        ModelElement baseWidget = addBaseWidgetRelation(model, aaiModel);
        generateWidgetChildren(baseWidget, model.getWidgets());
        return getModelAsString(aaiModel);
    }

    /**
     * Create a JAXB Model from the supplied Service or Resource.
     *
     * @param model
     *            the Service or Resource containing the model details
     * @return a new Model object based on the A&AI schema
     */
    private org.onap.aai.babel.xml.generator.xsd.Model createJaxbModel(Model model) {
        log.debug(model.toString());

        org.onap.aai.babel.xml.generator.xsd.Model aaiModel = new org.onap.aai.babel.xml.generator.xsd.Model();
        aaiModel.setModelInvariantId(model.getModelId());
        aaiModel.setModelType(model.getModelTypeName());

        aaiModel.setModelVers(new ModelVers());
        aaiModel.getModelVers().getModelVer().add(createModelVersion(model));

        return aaiModel;
    }

    /**
     * Create a new JAXB object representing the model-ver complex type, and populate this with the Model Version
     * information.
     * 
     * @param model
     *            the Service or Resource containing the version details
     * @return a new ModelVer object
     */
    private ModelVer createModelVersion(Model model) {
        ModelVer modelVer = new ModelVer();
        modelVer.setModelDescription(model.getModelDescription());
        modelVer.setModelName(model.getModelName());
        modelVer.setModelVersion(model.getModelVersion());
        modelVer.setModelVersionId(model.getModelNameVersionId());
        modelVer.setModelElements(new ModelElements());
        return modelVer;
    }

    /**
     * Add base widget model element for the Service or Resource.
     * 
     * @param model
     *            the Service or Resource containing the Model and child resources
     * @param aaiModel
     *            the JAXB Model to populate
     * @return a new ModelElement for the relationship to the base Widget
     * @throws XmlArtifactGenerationException
     */
    private ModelElement addBaseWidgetRelation(Model model, org.onap.aai.babel.xml.generator.xsd.Model aaiModel)
            throws XmlArtifactGenerationException {
        ModelElement widgetElement = createWidgetRelationshipModelElement(model);
        ModelVer modelVer = aaiModel.getModelVers().getModelVer().get(0);
        modelVer.getModelElements().getModelElement().add(widgetElement);

        // Add the child resources to the base widget model element list
        List<ModelElement> modelElements = widgetElement.getModelElements().getModelElement();
        for (Resource resource : model.getResources()) {
            modelElements.add(createRelationshipModelElement(resource));
        }

        return widgetElement;
    }

    /**
     * Create a model-element complex type storing the relationship to a Service or Resource model's base Widget.
     * 
     * @param model
     *            the Service or Resource model storing the widget's relationship data
     * @return a new Java object for the model-element type storing the Widget relationship
     * @throws XmlArtifactGenerationException
     */
    private ModelElement createWidgetRelationshipModelElement(Model model) throws XmlArtifactGenerationException {
        return createRelationshipModelElement(model.getDeleteFlag(), model.getWidgetId(),
                model.getWidgetInvariantId());
    }

    /**
     * Create a model-element complex type storing the relationship to a Resource model.
     * 
     * @param resource
     *            the Resource model storing the relationship data
     * @return a new Java object for the model-element type storing the relationship
     * @throws XmlArtifactGenerationException
     */
    private ModelElement createRelationshipModelElement(Resource resource) {
        return createRelationshipModelElement(resource.getDeleteFlag(), resource.getModelNameVersionId(),
                resource.getModelId());
    }

    /**
     * Create a model-element complex type storing the relationship to a Widget model.
     * 
     * @param widget
     *            the Widget model storing the relationship data
     * @return a new Java object for the model-element type storing the Widget relationship
     */
    private ModelElement createRelationshipModelElement(Widget widget) {
        return createRelationshipModelElement(widget.getDeleteFlag(), widget.getId(), widget.getWidgetId());
    }

    /**
     * Method to create the <model-element></model-element> holding the relationship value for a resource/widget model.
     *
     * @param newDataDelFlag
     *            new-data-del-flag (mapped from boolean to the string T or F)
     * @param modelVersionId
     *            model-version-id
     * @param modelInvariantId
     *            model-invariant-id
     * @return a new Java object for the model-element type storing the relationship
     */
    private ModelElement createRelationshipModelElement(boolean newDataDelFlag, String modelVersionId,
            String modelInvariantId) {
        ModelElement relationshipModelElement = new ModelElement();
        relationshipModelElement.setNewDataDelFlag(newDataDelFlag ? "T" : "F");
        relationshipModelElement.setCardinality("unbounded");
        relationshipModelElement.setModelElements(new ModelElements());
        relationshipModelElement.setRelationshipList(createModelRelationship(modelVersionId, modelInvariantId));
        return relationshipModelElement;
    }

    /**
     * Create the Model Version relationship data.
     * 
     * @param modelVersionId
     *            model-version-id
     * @param modelInvariantId
     *            model-invariant-id
     * @return a new RelationshipList object containing the model-ver relationships
     */
    private RelationshipList createModelRelationship(String modelVersionId, String modelInvariantId) {
        Relationship relationship = new Relationship();
        relationship.setRelatedTo("model-ver");
        List<RelationshipData> relationshipDataList = relationship.getRelationshipData();
        relationshipDataList.add(createRelationshipData("model-ver.model-version-id", modelVersionId));
        relationshipDataList.add(createRelationshipData("model.model-invariant-id", modelInvariantId));

        RelationshipList relationShipList = new RelationshipList();
        relationShipList.getRelationship().add(relationship);
        return relationShipList;
    }

    /**
     * Create a new RelationshipData element for the given key/value pair.
     * 
     * @param key
     *            relationship key
     * @param value
     *            relationship value
     * @return a new Java object representing the relationship-data complex type
     */
    private RelationshipData createRelationshipData(String key, String value) {
        RelationshipData data = new RelationshipData();
        data.setRelationshipKey(key);
        data.setRelationshipValue(value);
        return data;
    }

    /**
     * Method to create the child model elements of the widget. Handles the generation of recursive child widget
     * elements (if any).
     * 
     * @param parent
     *            Reference to the parent widget model element
     * @param widgets
     *            Set of child widgets obtained from the tosca/widget definition
     */
    private void generateWidgetChildren(ModelElement parent, Collection<Widget> widgets) {
        for (Widget widget : widgets) {
            ModelElement childRelation = createRelationshipModelElement(widget);
            parent.getModelElements().getModelElement().add(childRelation);
            // Recursive call to create any child widgets.
            generateWidgetChildren(childRelation, widget.getWidgets());
        }
    }

    /**
     * JAXB marshalling helper method to convert the Java object model to XML String.
     *
     * @param model
     *            Java Object model of a service/widget/resource
     * @return XML representation of the Java model in String format
     */
    private String getModelAsString(org.onap.aai.babel.xml.generator.xsd.Model model) {
        JAXBContext jaxbContext;
        StringWriter modelStringWriter = new StringWriter();
        try {
            jaxbContext = JAXBContext.newInstance(org.onap.aai.babel.xml.generator.xsd.Model.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "US-ASCII");
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            jaxbMarshaller.marshal(model, modelStringWriter);
        } catch (JAXBException jaxbException) {
            log.error(ApplicationMsgs.INVALID_CSAR_FILE, jaxbException);
            throw new DOMException(DOMException.SYNTAX_ERR, jaxbException.getMessage());
        }

        return modelStringWriter.toString();
    }
}
