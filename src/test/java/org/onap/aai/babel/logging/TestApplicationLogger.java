/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.aai.babel.logging;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.att.eelf.configuration.EELFLogger.Level;
import com.att.eelf.configuration.EELFManager;
import com.att.eelf.i18n.EELFResourceManager;
import jakarta.servlet.ServletRequest;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.logging.LogHelper.TriConsumer;
import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.mdc.MdcOverride;

/**
 * Tests for {@link LogHelper} and the {@link ApplicationMsgs} message resources.
 *
 * <p>
 * This test was previously {@code @Disabled} because it tailed the physical EELF log files (which are written
 * asynchronously via logback {@code AsyncAppender}s) and so failed intermittently depending on the runtime
 * environment and flush timing. It has been rewritten to assert deterministically:
 * <ul>
 * <li>the message-resource binding is verified directly against {@link EELFResourceManager} (no file I/O);</li>
 * <li>the logging methods are exercised as smoke tests asserting only that they do not throw.</li>
 * </ul>
 */
public class TestApplicationLogger {

    @BeforeAll
    public static void setupClass() {
        System.setProperty("APP_HOME", ".");
    }

    /**
     * Assert that every {@link ApplicationMsgs} enumeration value resolves to a message-format resource in
     * babel-logging-resources.properties.
     *
     * <p>
     * {@link EELFResourceManager#format} returns a sentinel string (error code {@code EELF9998E}, "... cannot be
     * formatted - no resource with that id exists") for an enum value that has no backing resource, so the formatted
     * message starting with the message's own Babel identifier proves the resource is present and correctly wired.
     * This catches the real failure mode the old test guarded against (an enum value added without a corresponding
     * properties entry, or vice versa) without reading any log file.
     */
    @Test
    public void everyMessageHasAResource() {
        for (ApplicationMsgs msg : ApplicationMsgs.values()) {
            String identifier = EELFResourceManager.getIdentifier(msg);
            assertThat("ApplicationMsgs." + msg.name() + " must have a resource identifier", identifier,
                    is(notNullValue()));
            assertThat("ApplicationMsgs." + msg.name() + " identifier should be a Babel message code", identifier,
                    startsWith("BABEL"));

            String formatted = EELFResourceManager.format(msg);
            assertThat("ApplicationMsgs." + msg.name() + " must resolve to its own message resource", formatted,
                    startsWith(identifier));
            assertThat("ApplicationMsgs." + msg.name() + " resource must exist", formatted,
                    not(containsString("cannot be formatted")));
        }
    }

    /**
     * Smoke test that each severity of the application logger can be invoked for every message without throwing.
     */
    @Test
    public void logAllMessages() {
        assertDoesNotThrow(() -> {
            Logger logger = LogHelper.INSTANCE;
            LogHelper.INSTANCE.clearContext();
            String[] args = {"1", "2", "3", "4"};
            for (ApplicationMsgs msg : ApplicationMsgs.values()) {
                if (msg.name().endsWith("ERROR")) {
                    logger.error(msg, args);
                    logger.error(msg, new RuntimeException("test exception"), args);
                } else {
                    logger.info(msg, args);
                    logger.warn(msg, args);
                }
                logger.debug(msg, args);
            }
        });
    }

    @Test
    public void logDebugMessage() {
        assertDoesNotThrow(() -> LogHelper.INSTANCE.debug("a message"));
    }

    @Test
    public void logTraceMessage() {
        assertDoesNotThrow(() -> {
            EELFManager.getInstance().getDebugLogger().setLevel(Level.TRACE);
            LogHelper.INSTANCE.trace(ApplicationMsgs.LOAD_PROPERTIES, "a message");
            EELFManager.getInstance().getAuditLogger().setLevel(Level.INFO);
            LogHelper.INSTANCE.trace(ApplicationMsgs.LOAD_PROPERTIES, "message not written");
        });
    }

    /**
     * Call logAuditError() for code coverage stats.
     */
    @Test
    public void logAuditError() {
        assertDoesNotThrow(() -> {
            LogHelper.INSTANCE.logAuditError(new Exception("test"));
            EELFManager.getInstance().getAuditLogger().setLevel(Level.OFF);
            LogHelper.INSTANCE.logAuditError(new Exception("test"));
            EELFManager.getInstance().getAuditLogger().setLevel(Level.INFO);
        });
    }

    /**
     * Smoke test logAudit with HTTP headers (exercises the request-header extraction path).
     */
    @Test
    public void logAuditMessage() {
        assertDoesNotThrow(() -> {
            final LogHelper logger = LogHelper.INSTANCE;

            @SuppressWarnings("unchecked")
            MultivaluedMap<String, String> headers = Mockito.mock(MultivaluedMap.class);
            Mockito.when(headers.getFirst("X-ECOMP-RequestID")).thenReturn("ecomp-request-id");
            Mockito.when(headers.getFirst("X-FromAppId")).thenReturn("app-id");

            // Call logAudit without first calling startAudit
            logger.logAuditSuccess("first call: bob");

            // This time call the start method
            logger.startAudit(headers, null);
            logger.logAuditSuccess("second call: foo");
        });
    }

    /**
     * Smoke test logAudit with no HTTP headers.
     */
    @Test
    public void logAuditMessageWithoutHeaders() {
        assertDoesNotThrow(() -> {
            LogHelper logger = LogHelper.INSTANCE;
            logger.startAudit(null, null);
            logger.logAuditSuccess("foo");
        });
    }

    /**
     * Smoke test logAudit with a mocked Servlet request.
     */
    @Test
    public void logAuditMessageWithServletRequest() {
        assertDoesNotThrow(() -> {
            ServletRequest servletRequest = Mockito.mock(ServletRequest.class);
            LogHelper logger = LogHelper.INSTANCE;
            logger.startAudit(null, servletRequest);
            logger.logAuditSuccess("foo");
        });
    }

    @Test
    public void setDefaultContextValue() {
        assertDoesNotThrow(() -> {
            LogHelper logger = LogHelper.INSTANCE;
            logger.setDefaultContextValue("key", "value");
            logger.setDefaultContextValue(MdcParameter.USER, null);
        });
    }

    /**
     * Smoke test logMetrics.
     */
    @Test
    public void logMetricsMessage() {
        assertDoesNotThrow(() -> LogHelper.INSTANCE.logMetrics("metrics: fred"));
    }

    @Test
    public void logMetricsMessageWithStopwatch() {
        assertDoesNotThrow(() -> {
            LogHelper logger = LogHelper.INSTANCE;
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            logger.logMetrics(stopWatch, "joe", "bloggs");
        });
    }

    @Test
    public void callUnsupportedMethods() {
        LogHelper logger = LogHelper.INSTANCE;
        ApplicationMsgs dummyMsg = ApplicationMsgs.LOAD_PROPERTIES;
        callUnsupportedOperationMethod(logger::error, dummyMsg);
        callUnsupportedOperationMethod(logger::info, dummyMsg);
        callUnsupportedOperationMethod(logger::warn, dummyMsg);
        callUnsupportedOperationMethod(logger::debug, dummyMsg);
        callUnsupportedOperationMethod(logger::trace, dummyMsg);
        try {
            logger.error(dummyMsg, new LogFields(), new RuntimeException("test"), "");
        } catch (UnsupportedOperationException e) {
            // Expected to reach here
        }
        try {
            logger.info(dummyMsg, new LogFields(), new MdcOverride(), "");
        } catch (UnsupportedOperationException e) {
            // Expected to reach here
        }
        try {
            logger.formatMsg(dummyMsg, "");
        } catch (UnsupportedOperationException e) {
            // Expected to reach here
        }
    }

    /**
     * Call a logger method which is expected to throw an UnsupportedOperationException.
     *
     * @param logMethod
     *            the logger method to invoke
     * @param dummyMsg
     *            any Application Message enumeration value
     */
    private void callUnsupportedOperationMethod(TriConsumer<Enum<?>, LogFields, String[]> logMethod,
            ApplicationMsgs dummyMsg) {
        try {
            logMethod.accept(dummyMsg, new LogFields(), new String[] {""});
            Assertions.fail("method should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected to reach here
        }
    }
}
