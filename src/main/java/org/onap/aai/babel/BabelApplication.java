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

package org.onap.aai.babel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ImportResource("classpath:babel-beans.xml")
@ComponentScan(basePackages = {
        "org.onap.aai.aaf",
        "org.onap.aai.babel"
})
@OpenAPIDefinition(
  info = @Info(
    title = "ONAP AAI Babel APIs",
    description = "Generates AAI model XML from SDC TOSCA artifacts",
    version = "1.0.0"
  )
)
public class BabelApplication extends SpringBootServletInitializer {

    private static ConfigurableApplicationContext context;

    /**
     * Spring Boot Initialization.
     *
     * @param args
     *            main args (expected to be null)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BabelApplication.class);
        app.setLogStartupInfo(false);
        app.setRegisterShutdownHook(true);
        context = app.run(args);
    }

    public static void exit() {
        SpringApplication.exit(context);
    }
}
