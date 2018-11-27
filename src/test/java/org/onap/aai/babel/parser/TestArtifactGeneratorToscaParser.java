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

package org.onap.aai.babel.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.xml.generator.data.WidgetConfigurationUtil;
import org.onap.aai.babel.xml.generator.model.AllotedResource;
import org.onap.aai.babel.xml.generator.model.InstanceGroup;
import org.onap.aai.babel.xml.generator.model.Resource;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.toscaparser.api.Group;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.SubstitutionMappings;

/**
 * Direct tests of the TOSCA parser-based Artifact Generator {@link ArtifactGeneratorToscaParser}., to cover exceptional
 * cases.
 */

public class TestArtifactGeneratorToscaParser {

	private static final String TEST_UUID = "1234";

	/**
	 * Process an Allotted Resource that does not have a Providing Service.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testMissingProvidingService() {
		List<NodeTemplate> nodeTemplateList = Collections.singletonList(buildNodeTemplate("name", "BlockStorage"));
		new ArtifactGeneratorToscaParser(null).processResourceModels(new AllotedResource(), nodeTemplateList);
	}

	/**
	 *
	 * Add a CR (a type of Resource which is not a Providing Service) to a Resource Model.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testAddResourceNotProvidingService() {
		List<NodeTemplate> nodeTemplateList = Collections.singletonList(buildNodeTemplate("testCR", "CR"));
		final Resource dummyResource = new AllotedResource(); // Any Resource to which the CR can be added
		new ArtifactGeneratorToscaParser(null).processResourceModels(dummyResource, nodeTemplateList);
	}

	/**
	 * Process a dummy Group object for a Service Resource.
	 */
	@Test
	public void testInstanceGroups() {
		final String instanceGroupType = "org.openecomp.groups.ResourceInstanceGroup";
		Properties props = new Properties();
		props.put("AAI.instance-group-types", instanceGroupType);
		WidgetConfigurationUtil.setFilterConfig(props);

		ISdcCsarHelper helper = Mockito.mock(ISdcCsarHelper.class);
		SubstitutionMappings sm = Mockito.mock(SubstitutionMappings.class);

		NodeTemplate serviceNodeTemplate = buildNodeTemplate("service",
				"org.openecomp.resource.cr.a-collection-resource");
		serviceNodeTemplate.setSubMappingToscaTemplate(sm);
		Mockito.when(helper.getNodeTemplateByName(serviceNodeTemplate.getName())).thenReturn(serviceNodeTemplate);

		ArrayList<Group> groups = new ArrayList<>();
		groups.add(buildGroup("group", instanceGroupType));
		Mockito.when(helper.getGroupsOfOriginOfNodeTemplate(serviceNodeTemplate)).thenReturn(groups);

		ArtifactGeneratorToscaParser parser = new ArtifactGeneratorToscaParser(helper);
		List<Resource> resources = parser.processInstanceGroups(new InstanceGroup(), serviceNodeTemplate);

		assertThat(resources.size(), is(1));
		Resource resource = resources.get(0);
		assertThat(resource.getModelNameVersionId(), is(equalTo(TEST_UUID)));
	}

	/**
	 * Create a NodeTemplate for unit testing purposes. In production code this object would only be created by the
	 * sdc-tosca parser.
	 *
	 * @param name
	 *            name of the NodeTemplate
	 * @param type
	 *            type of the NodeTemplate
	 * @return a new NodeTemplate object
	 */
	private NodeTemplate buildNodeTemplate(String name, String type) {
		LinkedHashMap<String, Object> nodeTemplateMap = new LinkedHashMap<>();
		nodeTemplateMap.put(name, buildMap("type", type));
		nodeTemplateMap.put(type, buildNodeTemplateCustomDefs());
		return new NodeTemplate(name, nodeTemplateMap, nodeTemplateMap, null, null);
	}

	private LinkedHashMap<String, Object> buildNodeTemplateCustomDefs() {
		LinkedHashMap<String, Object> customDefs = buildCustomDefs();
		customDefs.put("attributes", null);
		customDefs.put("requirements", null);
		customDefs.put("capabilities", null);
		customDefs.put("artifacts", null);
		return customDefs;
	}

	private Group buildGroup(String name, String type) {
		LinkedHashMap<String, Object> template = new LinkedHashMap<>();
		template.put("type", type);
		template.put("metadata", new LinkedHashMap<>());
		template.put("properties", buildMap("UUID", TEST_UUID));
		LinkedHashMap<String, Object> customDefMap = buildMap(name, template);
		customDefMap.put(type, buildGroupCustomDefs());
		return new Group(name, template, null, customDefMap);
	}

	private LinkedHashMap<String, Object> buildGroupCustomDefs() {
		LinkedHashMap<String, Object> customDefs = buildCustomDefs();
		customDefs.put("members", null);
		return customDefs;
	}

	private LinkedHashMap<String, Object> buildCustomDefs() {
		LinkedHashMap<String, Object> customDefs = new LinkedHashMap<>();
		customDefs.put("derived_from", null);
		customDefs.put("metadata", null);
		customDefs.put("version", null);
		customDefs.put("description", null);
		customDefs.put("interfaces", null);
		customDefs.put("properties", buildMap("UUID", buildMap("type", "java.lang.String")));
		return customDefs;
	}

	private LinkedHashMap<String, Object> buildMap(String key, Object value) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}
}
