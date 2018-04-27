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
package org.onap.aai.babel.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.Test;

public class TestInfoService {

    @Test
    public void testInitialisedInfoService() {
        String info = new InfoService().getInfo("");
        assertThat(info, startsWith("Status: Up\n"));
        assertThat(info, containsString("Started at"));
        assertThat(info, containsString("total=1"));
    }

    @Test
    public void testStatusReport() {
        InfoService infoService = new InfoService();
        LocalDateTime now = LocalDateTime.now();
        Clock clock = buildClock(now);

        String info = infoService.statusReport(clock);
        assertThat(info, containsString("Started at"));
        assertThat(info, containsString("total=1"));

        // Skip ahead 1 day
        clock = buildClock(now.plusDays(1));
        info = infoService.statusReport(clock);
        assertThat(info, containsString("Up time 1 day "));
        assertThat(info, containsString("total=2"));

        // Skip ahead 5 days
        clock = buildClock(now.plusDays(5));
        info = infoService.statusReport(clock);
        assertThat(info, containsString("Up time 5 days "));
        assertThat(info, containsString("total=3"));
    }

    private Clock buildClock(LocalDateTime now) {
        return Clock.fixed(now.toInstant(OffsetDateTime.now().getOffset()), Clock.systemDefaultZone().getZone());
    }

}
