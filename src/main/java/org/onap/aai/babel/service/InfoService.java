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
package org.onap.aai.babel.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.springframework.stereotype.Service;

/**
 * Information service for the micro-service. Return status details to the caller.
 *
 * @exclude
 */
@Path("/core/core-service")
@Service
public class InfoService {

    private Clock clock = Clock.systemDefaultZone();
    private LocalDateTime startTime = LocalDateTime.now(clock);
    private long infoCount = 0L;

    /**
     * @param format is an optional setting - html requests an HTML format
     * @return a formatted status report
     */
    @GET
    @Path("/info")
    @Produces("text/plain")
    public String getInfo(@DefaultValue("text") @QueryParam("format") String format) {
        return "Status: Up\n" + statusReport(clock) + "\n";
    }

    /** @return a status report showing the up time for the service */
    public String statusReport(Clock clock) {
        Temporal reportTime = LocalDateTime.now(clock);
        long upTime = ChronoUnit.SECONDS.between(startTime, reportTime);
        long upTimeDays = ChronoUnit.DAYS.between(startTime, reportTime);

        StringBuilder sb = new StringBuilder("Started at ");
        sb.append(startTime).append('\n').append("Up time ");
        if (upTimeDays > 0) {
            sb.append(upTimeDays).append(" day");
            if (upTimeDays > 1) {
                sb.append("s");
            }
            sb.append(" ");
        }
        sb.append(LocalTime.MIDNIGHT.plusSeconds(upTime).format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append('\n');

        sb.append('\n').append("Info Service").append('\n');
        sb.append("total=").append(++infoCount).append('\n');

        return sb.toString();
    }
}
