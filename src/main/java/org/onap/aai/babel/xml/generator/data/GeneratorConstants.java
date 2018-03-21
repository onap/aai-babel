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
package org.onap.aai.babel.xml.generator.data;

public class GeneratorConstants {

    /*
     * Private constructor to prevent instantiation
     */
    private GeneratorConstants() {
        throw new UnsupportedOperationException("This static class should not be instantiated!");
    }

    public static final String PROPERTY_ARTIFACT_GENERATOR_CONFIG_FILE = "artifactgenerator.config";

    public static final String VERSION = "version";
    public static final String CATEGORY = "category";
    public static final String SUBCATEGORY = "subcategory";
    public static final int ID_LENGTH = 36;

    public static final String GENERATOR_AAI_GENERATED_ARTIFACT_EXTENSION = "xml";

    // Error codes
    public static final String GENERATOR_INVOCATION_ERROR_CODE = "ARTIFACT_GENERATOR_INVOCATION_ERROR";

    // Error Constants
    public static final String GENERATOR_ERROR_INVALID_CLIENT_CONFIGURATION = "Invalid Client Configuration";
    public static final String GENERATOR_ERROR_ARTIFACT_GENERATION_FAILED =
            "Unable to generate artifacts for the provided input";
    public static final String GENERATOR_ERROR_SERVICE_INSTANTIATION_FAILED =
            "Artifact Generation Service Instantiation failed";

    // AAI Generator Error Messages
    public static final String GENERATOR_AAI_ERROR_CHECKSUM_MISMATCH = "Checksum Mismatch for file : %s";
    public static final String GENERATOR_AAI_ERROR_INVALID_TOSCA = "Invalid format for Tosca YML  : %s";
    public static final String GENERATOR_AAI_ERROR_UNSUPPORTED_WIDGET_OPERATION = "Operation Not Supported for Widgets";
    public static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_TOSCA =
            "Service tosca missing from list of input artifacts";
    public static final String GENERATOR_AAI_ERROR_NULL_RESOURCE_VERSION_IN_SERVICE_TOSCA =
            "Invalid Service definition mandatory attribute version missing for resource with UUID: <%s>";

    public static final String GENERATOR_AAI_ERROR_INVALID_RESOURCE_VERSION_IN_SERVICE_TOSCA =
            "Cannot generate artifacts. Invalid Resource version in Service tosca for resource with " + "UUID: "
                    + "<%s>";
    public static final String GENERATOR_AAI_ERROR_MISSING_RESOURCE_TOSCA =
            "Cannot generate artifacts. Resource Tosca missing for resource with UUID: <%s>";

    public static final String GENERATOR_AAI_ERROR_MISSING_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is not specified";

    public static final String GENERATOR_AAI_INVALID_SERVICE_VERSION =
            "Cannot generate artifacts. Service version is incorrect";

    // Logging constants
    public static final String PARTNER_NAME = "userId";
    public static final String ERROR_CATEGORY = "ErrorCategory";
    public static final String ERROR_CODE = "ErrorCode";
    public static final String ERROR_DESCRIPTION = "ErrorDescription";

    public static final String GENERATOR_ERROR_CODE = "300F";
    public static final String GENERATOR_PARTNER_NAME = "SDC Catalog";

    // AAI Generator Error Messages for Logging
    public static final String GENERATOR_AAI_CONFIGFILE_NOT_FOUND =
            "Cannot generate artifacts. Artifact Generator Configuration file not found at %s";
    public static final String GENERATOR_AAI_CONFIGLOCATION_NOT_FOUND =
            "Cannot generate artifacts. artifactgenerator.config system property not configured";
    public static final String GENERATOR_AAI_CONFIGLPROP_NOT_FOUND =
            "Cannot generate artifacts. Widget configuration not found for %s";
    public static final String GENERATOR_AAI_PROVIDING_SERVICE_MISSING =
            "Cannot generate artifacts. Providing Service is missing for allotted resource %s";
    public static final String GENERATOR_AAI_PROVIDING_SERVICE_METADATA_MISSING =
            "Cannot generate artifacts. Providing Service Metadata is missing for allotted resource %s";
}
