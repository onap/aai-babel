/**
 * ﻿============LICENSE_START=======================================================
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

import com.google.gson.annotations.SerializedName;

/**
 * This class represents Vendor Image data gleaned from tosca files within a csar file.
 *
 * <p>
 * Example: Where the value application property is 'VM00' this comes from the VNFConfiguration NodeTemplate under a
 * path:
 *
 * <pre>
 * node_templates:
 *     vFW_VNF_Configuration:
 *         type: org.openecomp.nodes.VnfConfiguration
 *         properties:
 *             allowed_flavors:
 *                 ATT_part_12345_for_FortiGate-VM00: Note the value of this element is dynamic
 *                     vendor_info:
 *                         vendor_model: VM00
 * </pre>
 *
 * Where the value applicationVendor property is 'ATT (Tosca)' this comes from the VNFConfiguration NodeTemplate under a
 * path:
 *
 * <pre>
 * node_templates:
 *     vFW_VNF_Configuration:
 *         type: org.openecomp.nodes.VnfConfiguration
 *         metadata:
 *             resourceVendor: ATT (Tosca)
 * </pre>
 *
 * Where the value applicationVersion property is '3.16.9' this comes from the MultiFlavorVFC NodeTemplate under a path:
 *
 * <pre>
 *  node_templates:
 *     vWAN_VFC:
 *         type: org.openecomp.resource.abstract.nodes.MultiFlavorVFC
 *         properties:
 *             images:
 *                 3.16.1: Note the value of this element is dynamic - represents the name of each image
 *                     software_version: 3.16.1
 * </pre>
 */
class VendorImageConfiguration {
    private String application;

    @SerializedName("application-vendor")
    private String applicationVendor;

    @SerializedName("application-version")
    private String applicationVersion;

    VendorImageConfiguration(String application, String applicationVendor, String applicationVersion) {
        this.application = application;
        this.applicationVendor = applicationVendor;
        this.applicationVersion = applicationVersion;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getApplicationVendor() {
        return applicationVendor;
    }

    public void setApplicationVendor(String applicationVendor) {
        this.applicationVendor = applicationVendor;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }
}
