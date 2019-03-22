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

package org.onap.aai.babel.csar.vnfcatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onap.aai.babel.logging.ApplicationMsgs;
import org.onap.aai.babel.logging.LogHelper;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.enums.SdcTypes;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcPropertyNames;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.NodeTemplate;

/**
 * This class is responsible for extracting Virtual Network Function (VNF) information from a TOSCA 1.1 CSAR package.
 *
 * <p>
 * CSAR is a compressed format that stores multiple TOSCA files. Each TOSCA file may define Topology Templates and/or
 * Node Templates along with other model data.
 * </p>
 *
 * <p>
 * A VNF is a virtualized functional capability (e.g. a Router) that may be defined by an external Vendor. Within the
 * model defined by the TOSCA files the VNF is considered to be a Resource (part of a Service).
 * </p>
 *
 * <p>
 * A VNF is specified by a Topology Template. Because this TOSCA construct does not allow properties to be defined
 * directly, Node Templates are defined (identified by a VNF type value) storing the VNF metadata and properties.
 * </p>
 *
 * <p>
 * A VNF may be composed of multiple images, each running on its own Virtual Machine. The function of a deployed image
 * is designated the Virtual Function Component (VFC). A VFC is a sub-component of the VNF. A VFC may have different
 * "Flavors" (see the Deployment Flavors description below).
 * </p>
 *
 * <p>
 * An individual VNF (template) may be deployed with varying configuration values, according to
 * environment/customer/business needs. For example, a VNF deployed in a testing environment would typically use fewer
 * computational resources than in a production setting.
 * </p>
 *
 * <p>
 * A Vendor may define one or more "Deployment Flavors". A Deployment Flavor describes a set of pre-determined
 * parameterised values for a specific aspect of the model. Within the TOSCA model there is a DeploymentFlavor
 * definition, which has its own data types, and also an ImageInfo definition.
 * </p>
 */
public class VnfVendorImageExtractor {

    private static LogHelper applicationLogger = LogHelper.INSTANCE;

    // The following constants describe the expected naming conventions for TOSCA Node Templates which
    // store the VNF configuration and the VFC data items.

    // The name of the property (for a VNF Configuration type) storing the Images Information
    private static final String IMAGES = "images";

    // Name of property key that contains the value of the software version
    private static final String SOFTWARE_VERSION = "software_version";

    // The name of the property (for a VNF Configuration type) storing the Vendor Information
    private static final String VNF_CONF_TYPE_PROPERTY_VENDOR_INFO_CONTAINER = "allowed_flavors";

    // Name of property key that contains the Vendor Information
    private static final String VENDOR_INFO = "vendor_info";

    // Name of property key that contains the value of model of the Vendor application
    private static final String VENDOR_MODEL = "vendor_model";

    /**
     * This method is responsible for parsing the vnfConfiguration entity in the same topology_template (there's an
     * assumption that there's only one per file, awaiting verification).
     *
     * <p>
     * It is possible that CSAR file does not contain a vnfConfiguration and this is valid.
     * </p>
     *
     * <p>
     * Where multiple vnfConfigurations are found an exception will be thrown.
     * </p>
     *
     * <p>
     * The ASDC model anticipates the following permutations of vnfConfiguration and multiflavorVFC:
     *
     * <pre>
     * <ol>
     * <li>Single vnfConfiguration, single multiFlavorVFC with multiple images.</li>
     * <li>Single vnfConfiguration, multi multiFlavorVFC with single images.</li>
     * </ol>
     * </pre>
     *
     * All ImageInfo sections found apply to all "Deployment Flavors", therefore we can apply a cross product of
     * "Deployment Flavors" x "ImageInfo"
     * </p>
     *
     * @param csar compressed format that stores multiple TOSCA files and in particular a vnfConfiguration
     * @return BabelArtifact VendorImageConfiguration objects created during processing represented as the Babel service
     *         public data structure
     * @throws ToscaToCatalogException if the CSAR content is not valid
     */
    public BabelArtifact extract(byte[] csar) throws ToscaToCatalogException {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        Objects.requireNonNull(csar, "A CSAR file must be supplied");
        applicationLogger.info(ApplicationMsgs.DISTRIBUTION_EVENT, "Extracting VNF Configuration data");

        List<VendorImageConfiguration> vendorImageConfigurations;
        Path path = null;

        try {
            path = createTempFile(csar);
            vendorImageConfigurations = createVendorImageConfigurations(path.toAbsolutePath().toString());
        } catch (InvalidNumberOfNodesException | IOException | SdcToscaParserException e) {
            throw new ToscaToCatalogException(
                    "An error occurred trying to get the VNF Catalog from a CSAR file. " + e.getLocalizedMessage(), e);
        } finally {
            if (path != null) {
                FileUtils.deleteQuietly(path.toFile());
            }
        }

        applicationLogger.info(ApplicationMsgs.DISTRIBUTION_EVENT, vendorImageConfigurations.toString());
        applicationLogger.logMetrics(stopwatch, LogHelper.getCallerMethodName(0));

        return ConfigurationsToBabelArtifactConverter.convert(vendorImageConfigurations);
    }

    /**
     * Creates a temporary file to store the CSAR content.
     *
     * @param bytes the CSAR content
     * @return Path to a temporary file containing the CSAR bytes
     * @throws IOException if an I/O error occurs or the temporary-file directory does not exist
     */
    private Path createTempFile(byte[] bytes) throws IOException {
        Path path = Files.createTempFile("temp", ".csar");
        applicationLogger.debug("Created temp file " + path);
        Files.write(path, bytes);
        return path;
    }

    /**
     * Build VNF Vendor Image Configurations for the VNF Configuration node (if present) in the CSAR file referenced by
     * the supplied Path.
     *
     * @param csarFilepath
     *            the path to the CSAR file
     * @return a List of Vendor Image Configurations
     * @throws SdcToscaParserException
     * @throws ToscaToCatalogException
     * @throws InvalidNumberOfNodesException
     */
    private List<VendorImageConfiguration> createVendorImageConfigurations(String csarFilepath)
            throws SdcToscaParserException, InvalidNumberOfNodesException {
        ISdcCsarHelper csarHelper = SdcToscaParserFactory.getInstance().getSdcCsarHelper(csarFilepath);

        List<NodeTemplate> serviceVfList = csarHelper.getServiceNodeTemplates().stream() //
                .filter(filterOnType(SdcTypes.VF)).collect(Collectors.toList());

        List<NodeTemplate> vnfConfigs = serviceVfList.stream()
                .flatMap(vf -> vf.getSubMappingToscaTemplate().getNodeTemplates().stream()
                        .filter(filterOnType(SdcTypes.VFC)) //
                        .filter(vfc -> vfc.getType().endsWith("VnfConfiguration")))
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());

        if (!vnfConfigs.isEmpty()) {
            NodeTemplate vnfConfigurationNode = vnfConfigs.get(0);

            applicationLogger.info(ApplicationMsgs.DISTRIBUTION_EVENT,
                    String.format("Found VNF Configuration node \"%s\"", vnfConfigurationNode));

            if (vnfConfigs.size() > 1) {
                throw new InvalidNumberOfNodesException("Only one VNF configuration node is allowed however "
                        + vnfConfigs.size() + " nodes were found in the CSAR.");
            }

            return createVendorImageConfigurations(serviceVfList, vnfConfigurationNode);
        }

        return Collections.emptyList();
    }

    /**
     * Build VNF Vendor Image Configurations for each Service VF, using the flavors of the specified VNF Configuration
     * node.
     *
     * @param serviceVfList
     *            the Service level VF node templates
     * @param vnfConfigurationNode
     *            the VNF node template
     * @return a new List of Vendor Image Configurations
     */
    private List<VendorImageConfiguration> createVendorImageConfigurations(List<NodeTemplate> serviceVfList,
            NodeTemplate vnfConfigurationNode) {
        List<VendorImageConfiguration> vendorImageConfigurations = Collections.emptyList();

        Object allowedFlavorProperties =
                vnfConfigurationNode.getPropertyValue(VNF_CONF_TYPE_PROPERTY_VENDOR_INFO_CONTAINER);

        if (allowedFlavorProperties instanceof Map) {
            @SuppressWarnings("unchecked")
            Collection<Map<String, Map<String, String>>> flavorMaps =
                    ((Map<?, Map<String, Map<String, String>>>) allowedFlavorProperties).values();

            vendorImageConfigurations = serviceVfList.stream() //
                    .flatMap(node -> buildVendorImageConfigurations(flavorMaps, node)) //
                    .collect(Collectors.toList());

            applicationLogger.info(ApplicationMsgs.DISTRIBUTION_EVENT,
                    "Built " + vendorImageConfigurations.size() + " Vendor Image Configurations");
        }

        return vendorImageConfigurations;
    }

    private Predicate<? super NodeTemplate> filterOnType(SdcTypes sdcType) {
        return node -> (node.getMetaData() != null
                && sdcType.getValue().equals(node.getMetaData().getValue(SdcPropertyNames.PROPERTY_NAME_TYPE)));
    }

    /**
     * Builds the Vendor Image configurations.
     *
     * @param flavorMaps
     *            the collection of flavors and the properties for those flavors
     * @param vfNodeTemplate
     *            the node template for the VF
     *
     * @return a stream of VendorImageConfiguration objects
     */
    private Stream<VendorImageConfiguration> buildVendorImageConfigurations(
            Collection<Map<String, Map<String, String>>> flavorMaps, NodeTemplate vfNodeTemplate) {
        String resourceVendor = vfNodeTemplate.getMetaData().getValue("resourceVendor");
        applicationLogger.debug("Resource Vendor " + resourceVendor);

        List<String> softwareVersions =
                extractSoftwareVersions(vfNodeTemplate.getSubMappingToscaTemplate().getNodeTemplates());
        applicationLogger.debug("Software Versions: " + softwareVersions);

        return flavorMaps.stream() //
                .map(value -> value.entrySet().stream() //
                        .filter(entry -> VENDOR_INFO.equals(entry.getKey())) //
                        .map(e -> e.getValue().get(VENDOR_MODEL)) //
                        .findFirst()) //
                .flatMap(vendorModel -> softwareVersions.stream().map(
                        version -> new VendorImageConfiguration(vendorModel.orElse(null), resourceVendor, version)));
    }

    /**
     * Extract Image software versions from node templates.
     *
     * @param nodeTemplates
     *            the node templates to search for software versions
     * @return a List of Software Version Strings
     */
    @SuppressWarnings("unchecked")
    List<String> extractSoftwareVersions(Collection<NodeTemplate> nodeTemplates) {
        return nodeTemplates.stream() //
                .filter(nodeTemplate -> nodeTemplate.getPropertyValue(IMAGES) != null) //
                .flatMap(imagesNode -> ((Map<String, Object>) imagesNode.getPropertyValue(IMAGES)).entrySet().stream())
                .map(property -> findSoftwareVersion((Map<String, Object>) property.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Get the first software version value from the properties Map.
     *
     * @param image the properties Map
     * @return the software version value as a String
     */
    private String findSoftwareVersion(Map<String, Object> image) {
        applicationLogger.debug("Finding " + SOFTWARE_VERSION + " from " + image);

        return (String) image.entrySet().stream()//
                .filter(entry -> SOFTWARE_VERSION.equals(entry.getKey())) //
                .map(Entry::getValue).findFirst().orElse(null);
    }
}
