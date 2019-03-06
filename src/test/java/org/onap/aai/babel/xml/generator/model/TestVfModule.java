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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.model.Widget.Type;

/**
 * Direct tests of the VFMODULE Resource and Widget functionality to improve code coverage.
 */
public class TestVfModule {

    /**
     * Load the Widget Configuration, including the type mappings and the UUID mappings.
     *
     * @throws IOException
     *             if the mappings configuration cannot be loaded
     */
    @BeforeClass
    public static void setup() throws IOException {
        ArtifactTestUtils util = new ArtifactTestUtils();
        util.loadWidgetToUuidMappings();
        util.loadWidgetMappings();
    }

    /**
     * Call hashCode() method for code coverage.
     */
    @Test
    public void testHashCode() {
        Resource vfModule = createNewVfModule();
        populateIdentInfo(vfModule);
        assertThat(vfModule.hashCode(), is(notNullValue()));
    }

    /**
     * Call equals() method for code coverage.
     */
    @Test
    public void testEquals() {
        Resource vfModuleA = createNewVfModule();
        populateIdentInfo(vfModuleA);

        // equals() is reflexive
        assertThat(vfModuleA.equals(vfModuleA), is(true));

        // equals() is symmetric
        Resource vfModuleB = createNewVfModule();
        populateIdentInfo(vfModuleB);
        assertThat(vfModuleA.equals(vfModuleB), is(true));
        assertThat(vfModuleB.equals(vfModuleA), is(true));

        assertThat(vfModuleA.equals(null), is(false));
    }

    @Test
    public void testAddVServerWidgetToVf() throws XmlArtifactGenerationException {
        assertAddWidget(createNewVfModule(), Type.VSERVER);
    }

    @Test
    public void testAddServiceWidgetToVf() throws XmlArtifactGenerationException {
        assertAddWidget(createNewVfModule(), Type.SERVICE);
    }

    /**
     * Add a new Widget to a VF Module, where the Widget is NOT set as a member. N.B. For the current VF Module
     * implementation the actual Widget type is not important.
     * 
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    @Test
    public void testNonMemberWidgetToVf() throws XmlArtifactGenerationException {
        Resource vfModule = createNewVfModule();
        assertThat(vfModule.addWidget(createNewWidget(Type.SERVICE)), is(false));
        assertNumberOfWidgets(vfModule, 0);
    }

    /**
     * OAM Network is specifically excluded from a VF Module.
     * 
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    @Test
    public void testAddOamNetworkWidgetToVf() throws XmlArtifactGenerationException {
        Resource vfModule = createNewVfModule();
        assertThat(createNewWidgetForModule(vfModule, Type.OAM_NETWORK), is(false));
        assertNumberOfWidgets(vfModule, 0);
    }

    /**
     * Add a Volume Widget to a VF Module via a vserver Widget.
     * 
     * <li>Create a VF Module</li>
     * <li>Add a Volume Widget</li>
     * <li>Add a vserver Widget</li>
     * <li>Check that the Volume Widget appears under the vserver</li>
     * 
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    @Test
    public void testAddVolumeWidgetToVf() throws XmlArtifactGenerationException {
        Resource vfModule = createNewVfModule();

        // Adding a Volume widget has no effect until a vserver widget is added.
        assertAddWidget(vfModule, Type.VOLUME);
        assertNumberOfWidgets(vfModule, 0);

        final int vserverBaseWidgetCount = createVserverForVf(vfModule);

        // The vserver now has Volume as well.
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 1);

        // Adding another instance of a vserver widget fails.
        assertFailToAddWidget(vfModule, Type.VSERVER);
        assertNumberOfWidgets(vfModule, 1);

        // Adding another Volume widget is always treated as successful.
        assertAddWidget(vfModule, Type.VOLUME);
        // Assert that no additional Widgets are actually present.
        assertNumberOfWidgets(vfModule, 1);
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 1);
    }

    /**
     * Add an L-Interface Widget to a VF Module via a vserver Widget.
     * 
     * <li>Create a VF Module</li>
     * <li>Add an L-Interface Widget</li>
     * <li>Add a vserver Widget</li>
     * <li>Check that the L-Interface Widget appears under the vserver</li>
     * 
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    @Test
    public void testAddLinterfaceWidgetToVf() throws XmlArtifactGenerationException {
        Resource vfModule = createNewVfModule();

        // Adding an L-Interface widget has no effect until a vserver widget is added.
        assertFailToAddWidget(vfModule, Type.LINT);
        assertNumberOfWidgets(vfModule, 0);

        final int vserverBaseWidgetCount = createVserverForVf(vfModule);

        // The vserver now has an L-Interface as well.
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 1);

        // Adding another instance of a vserver widget fails.
        assertFailToAddWidget(vfModule, Type.VSERVER);
        assertNumberOfWidgets(vfModule, 1);

        // Adding an L-Interface widget is always treated as successful when a vserver exists.
        assertAddWidget(vfModule, Type.LINT);
        // Assert that no additional Widgets are actually present.
        assertNumberOfWidgets(vfModule, 1);
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 1);
    }

    /**
     * Add a Volume and an L-Interface Widget to a VF Module via a vserver Widget.
     * 
     * <li>Create a VF Module</li>
     * <li>Add a Volume Widget</li>
     * <li>Add an L-Interface Widget</li>
     * <li>Add a vserver Widget</li>
     * <li>Check that both Widgets appear under the vserver</li>
     * 
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    @Test
    public void testAddVolumeAndLinterfaceWidgetToVf() throws XmlArtifactGenerationException {
        Resource vfModule = createNewVfModule();

        // Adding a Volume widget has no effect until a vserver widget is added.
        assertAddWidget(vfModule, Type.VOLUME);
        assertNumberOfWidgets(vfModule, 0);

        // Adding an L-Interface widget has no effect until a vserver widget is added.
        assertFailToAddWidget(vfModule, Type.LINT);
        assertNumberOfWidgets(vfModule, 0);

        final int vserverBaseWidgetCount = createVserverForVf(vfModule);

        // The vserver now has both Volume and L-Interface.
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 2);

        // Adding another instance of a vserver widget fails.
        assertFailToAddWidget(vfModule, Type.VSERVER);
        assertNumberOfWidgets(vfModule, 1);

        // Add new instances (with no effect).
        assertAddWidget(vfModule, Type.VOLUME);
        assertAddWidget(vfModule, Type.LINT);
        // Assert that no additional Widgets are in fact present.
        assertNumberOfWidgets(vfModule, 1);
        assertNumberOfWidgets(vfModule.vserver, vserverBaseWidgetCount + 2);
    }

    private void assertNumberOfWidgets(Model model, int numberOfWidgets) {
        assertThat(model.getWidgets(), hasSize(numberOfWidgets));
    }

    /**
     * Use the static Factory method to create a new Widget.
     *
     * @param widgetType
     *            type of Widget to create
     * @return a new Widget
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private Widget createNewWidget(Type widgetType) throws XmlArtifactGenerationException {
        return Widget.getWidget(widgetType);
    }

    /**
     * Create a new VF Module that contains zero widgets and has no members.
     *
     * @return new VF Module resource
     */
    private Resource createNewVfModule() {
        Resource vfModule = new Resource(Type.VFMODULE, true);
        assertNumberOfWidgets(vfModule, 0);
        return vfModule;
    }

    /**
     * Set up some dummy Model Identification properties.
     *
     * @param vfModule
     *            to be populated
     */
    private void populateIdentInfo(Resource vfModule) {
        Map<String, String> modelIdentInfo = new HashMap<>();
        modelIdentInfo.put("UUID", "dummy_uuid");
        vfModule.populateModelIdentificationInformation(modelIdentInfo);
    }

    /**
     * Create a new Widget and assert that it is successfully added to the VF Module.
     *
     * @param vfModule
     *            the VF Module to update
     * @param widgetType
     *            the type of Widget to create and add
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private void assertAddWidget(Resource vfModule, Type widgetType) throws XmlArtifactGenerationException {
        assertThat(createNewWidgetForModule(vfModule, widgetType), is(true));
    }

    /**
     * Create a new Widget and assert that it cannot be added to the VF Module.
     *
     * @param vfModule
     *            the VF Module
     * @param widgetType
     *            the type of Widget to create and attempt to add
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private void assertFailToAddWidget(Resource vfModule, Type widgetType) throws XmlArtifactGenerationException {
        assertThat(createNewWidgetForModule(vfModule, widgetType), is(false));
    }

    /**
     * Create a new widget, make it a member of the VF Module, then try to add it.
     *
     * @param vfModule
     *            the VF Module to update
     * @param widgetType
     *            the type of Widget to create and attempt to add
     * @return whether or not the Widget was added to the module
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private boolean createNewWidgetForModule(Resource vfModule, Type widgetType) throws XmlArtifactGenerationException {
        Widget widget = createNewWidget(widgetType);
        setWidgetAsMember(vfModule, widget);
        return vfModule.addWidget(widget);
    }

    /**
     * Make the specified Widget the sole member of the VF Module. This is achieved by first adding the Widget's own ID
     * to its set of keys, and by then setting the VF Module's members to a Singleton List comprised of this ID. These
     * updates allow the Widget to be successfully added to the VF Module. (Non-member Widgets cannot be added.)
     *
     * @param vfModule
     *            the module for which members are overwritten
     * @param widget
     *            the widget to be set as the member
     */
    private void setWidgetAsMember(Resource vfModule, Widget widget) {
        String id = widget.getId();
        widget.addKey(id);
        vfModule.setMembers(Collections.singletonList(id));
    }

    /**
     * Create a vserver widget and add it to the specified VF Module.
     *
     * @param vfModule
     *            the VF Module to update
     * @return the number of Widgets present in the vserver on creation
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private int createVserverForVf(Resource vfModule) throws XmlArtifactGenerationException {
        Widget vserverWidget = createNewWidget(Type.VSERVER);
        assertNumberOfWidgets(vfModule, 0);
        final int initialWidgetCount = addVserverToVf(vfModule, vserverWidget);
        assertNumberOfWidgets(vfModule, 1);
        return initialWidgetCount;
    }

    /**
     * Add the specified vserver to the specified VF Module.
     * 
     * @param vfModule
     *            the VF Module to update
     * @param vserverWidget
     *            the Widget to add
     * @return initial widget count for the vserver Widget
     * @throws XmlArtifactGenerationException
     *             if the Widget mapping configuration is missing
     */
    private int addVserverToVf(Resource vfModule, Widget vserverWidget) throws XmlArtifactGenerationException {
        // A vserver (initially) has Flavor, Image, Tenant and Vfc.
        final int initialWidgetCount = 4;
        assertNumberOfWidgets(vserverWidget, initialWidgetCount);

        // Add the vserver to the VF Module.
        setWidgetAsMember(vfModule, vserverWidget);
        assertThat(vfModule.addWidget(vserverWidget), is(true));

        return initialWidgetCount;
    }
}
