package org.onap.aai.babel.xml.generator.api;


import org.apache.groovy.util.Maps;
import org.junit.Test;
import org.onap.aai.babel.util.ArtifactTestUtils;
import org.onap.aai.babel.xml.generator.XmlArtifactGenerationException;
import org.onap.aai.babel.xml.generator.model.Model;
import org.onap.aai.babel.xml.generator.model.Service;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AaiModelGeneratorTest {

    private AaiModelGenerator generator = new AaiModelGenerator();

    @Test
    public void shouldGenerateModelWithInstantiationType() throws XmlArtifactGenerationException, IOException {
        new ArtifactTestUtils().loadWidgetMappings();
        Model model = new Service();
        model.populateModelIdentificationInformation(Maps.of("instantiationType", "macro"));


        String generatedXml = generator.generateModelFor(model);

        assertThat(generatedXml).containsSubsequence("   <model-vers>\n" +
                "        <model-ver>\n" +
                "            <orchestration-type>macro</orchestration-type>");
    }
}