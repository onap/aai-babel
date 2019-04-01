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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.att.eelf.configuration.EELFLogger.Level;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.babel.logging.LogHelper.MdcParameter;
import org.onap.aai.babel.logging.LogHelper.TriConsumer;
import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.mdc.MdcOverride;

/**
 * Simple test to log each of the validation messages in turn.
 *
 * This version tests only the error logger at INFO level.
 *
 */
public class TestApplicationLogger {

    @BeforeClass
    public static void setupClass() {
        System.setProperty("APP_HOME", ".");
    }

    /**
     * Check that each message can be logged and that (by implication of successful logging) there is a corresponding
     * resource (message format).
     *
     * @throws IOException
     *             if the log files cannot be read
     */
    @Test
    public void logAllMessages() throws IOException {
        Logger logger = LogHelper.INSTANCE;
        LogHelper.INSTANCE.clearContext();
        LogReader errorReader = new LogReader(LogHelper.getLogDirectory(), "error");
        LogReader debugReader = new LogReader(LogHelper.getLogDirectory(), "debug");
        String[] args = {"1", "2", "3", "4"};
        for (ApplicationMsgs msg : Arrays.asList(ApplicationMsgs.values())) {
            if (msg.name().endsWith("ERROR")) {
                logger.error(msg, args);
                validateLoggedMessage(msg, errorReader, "ERROR");

                logger.error(msg, new RuntimeException("fred"), args);
                validateLoggedMessage(msg, errorReader, "fred");
            } else {
                logger.info(msg, args);
                validateLoggedMessage(msg, errorReader, "INFO");

                logger.warn(msg, args);
                validateLoggedMessage(msg, errorReader, "WARN");
            }

            logger.debug(msg, args);
            validateLoggedMessage(msg, debugReader, "DEBUG");
        }
    }

    /**
     * Check that each message can be logged and that (by implication of successful logging) there is a corresponding
     * resource (message format).
     *
     * @throws IOException
     *             if the log file cannot be read
     */
    @Test
    public void logDebugMessages() throws IOException {
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "debug");
        LogHelper.INSTANCE.debug("a message");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
    }

    @Test
    public void logTraceMessage() throws IOException {
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "debug");
        EELFManager.getInstance().getDebugLogger().setLevel(Level.TRACE);
        LogHelper.INSTANCE.trace(ApplicationMsgs.LOAD_PROPERTIES, "a message");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        EELFManager.getInstance().getAuditLogger().setLevel(Level.INFO);
        LogHelper.INSTANCE.trace(ApplicationMsgs.LOAD_PROPERTIES, "message not written");
    }

    /**
     * Call logAuditError() for code coverage stats.
     */
    @Test
    public void logAuditError() {
        LogHelper.INSTANCE.logAuditError(new Exception("test"));
        EELFManager.getInstance().getAuditLogger().setLevel(Level.OFF);
        LogHelper.INSTANCE.logAuditError(new Exception("test"));
        EELFManager.getInstance().getAuditLogger().setLevel(Level.INFO);
    }

    /**
     * Check logAudit with HTTP headers.
     *
     * @throws IOException
     *             if the log file cannot be read
     */
    @Test
    public void logAuditMessage() throws IOException {
        final LogHelper logger = LogHelper.INSTANCE;
        final LogReader reader = new LogReader(LogHelper.getLogDirectory(), "audit");

        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.getHeaderString("X-ECOMP-RequestID")).thenReturn("ecomp-request-id");
        Mockito.when(headers.getHeaderString("X-FromAppId")).thenReturn("app-id");

        // Call logAudit without first calling startAudit
        logger.logAuditSuccess("first call: bob");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat("audit message log level", str, containsString("INFO"));
        assertThat("audit message content", str, containsString("bob"));

        // This time call the start method
        logger.startAudit(headers, null);
        logger.logAuditSuccess("second call: foo");
        str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat("audit message log level", str, containsString("INFO"));
        assertThat("audit message content", str, containsString("foo"));
        assertThat("audit message content", str, containsString("ecomp-request-id"));
        assertThat("audit message content", str, containsString("app-id"));
    }

    /**
     * Check logAudit with no HTTP headers.
     *
     * @throws IOException
     *             if the log file cannot be read
     */
    @Test
    public void logAuditMessageWithoutHeaders() throws IOException {
        LogHelper logger = LogHelper.INSTANCE;
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "audit");
        logger.startAudit(null, null);
        logger.logAuditSuccess("foo");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat("audit message log level", str, containsString("INFO"));
        assertThat("audit message content", str, containsString("foo"));
    }

    /**
     * Check logAudit with mocked Servlet request.
     *
     * @throws IOException
     *             if the log file cannot be read
     */
    @Test
    public void logAuditMessageWithServletRequest() throws IOException {
        ServletRequest servletRequest = Mockito.mock(ServletRequest.class);
        LogHelper logger = LogHelper.INSTANCE;
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "audit");
        logger.startAudit(null, servletRequest);
        logger.logAuditSuccess("foo");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat("audit message log level", str, containsString("INFO"));
        assertThat("audit message content", str, containsString("foo"));
    }

    @Test
    public void setDefaultContextValue() {
        LogHelper logger = LogHelper.INSTANCE;
        logger.setDefaultContextValue("key", "value");
        logger.setDefaultContextValue(MdcParameter.USER, null);
    }

    /**
     * Check logMetrics.
     *
     * @throws IOException
     *             if the log file cannot be read
     */
    @Test
    public void logMetricsMessage() throws IOException {
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "metrics");
        LogHelper logger = LogHelper.INSTANCE;
        logger.logMetrics("metrics: fred");
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat("metrics message log level", str, containsString("INFO"));
        assertThat("metrics message content", str, containsString("fred"));
    }

    @Test
    public void logMetricsMessageWithStopwatch() throws IOException {
        LogReader reader = new LogReader(LogHelper.getLogDirectory(), "metrics");
        LogHelper logger = LogHelper.INSTANCE;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.logMetrics(stopWatch, "joe", "bloggs");
        String logLine = reader.getNewLines();
        assertThat(logLine, is(notNullValue()));
        assertThat("metrics message log level", logLine, containsString("INFO"));
        assertThat("metrics message content", logLine, containsString("joe"));
    }

    @Test
    public void callUnsupportedMethods() throws IOException {
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
            org.junit.Assert.fail("method should have thrown execption"); // NOSONAR as code not reached
        } catch (UnsupportedOperationException e) {
            // Expected to reach here
        }
    }

    /**
     * Assert that a log message was logged to the expected log file at the expected severity.
     *
     * @param msg
     *            the Application Message enumeration value
     * @param reader
     *            the log reader for the message
     * @param severity
     *            log level
     * @throws IOException
     *             if the log file cannot be read
     */
    private void validateLoggedMessage(ApplicationMsgs msg, LogReader reader, String severity) throws IOException {
        String str = reader.getNewLines();
        assertThat(str, is(notNullValue()));
        assertThat(msg.toString() + " log level", str, containsString(severity));
    }
}
