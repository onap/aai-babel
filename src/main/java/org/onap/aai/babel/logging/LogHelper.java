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
package org.onap.aai.babel.logging;

import static com.att.eelf.configuration.Configuration.MDC_SERVICE_NAME;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.core.FileAppender;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.att.eelf.i18n.EELFResolvableErrorEnum;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.time.StopWatch;
import org.onap.aai.babel.request.RequestHeaders;
import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.cl.mdc.MdcOverride;
import org.onap.aai.restclient.client.Headers;
import org.slf4j.MDC;

/*-
 * This Log Helper mimics the interface of a Common Logging Logger
 * but adds helper methods for audit and metrics logging requirements.
 *
 * Messages are logged to the appropriate EELF functional logger as described below.
 *
 * Error Log: INFO/WARN/ERROR/FATAL
 * Debug Log: DEBUG/TRACE
 * Audit Log: summary view of transaction processing
 * Metrics Log: detailed timings of transaction processing interactions
 *
 * Audit and Metrics log messages record the following fields:
 *
 * RequestID   - an RFC4122 UUID for the transaction request
 * ServiceName - the API provided by this service
 * PartnerName - invoker of the API
 * ClassName   - name of the class creating the log record
 *
 * The above list is not exhaustive.
 */
public enum LogHelper implements Logger {
    INSTANCE; // Joshua Bloch's singleton pattern

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        public void accept(T t, U u, V v);
    }

    /** Audit log message status code values. See {@code MdcParameter.STATUS_CODE} */
    public enum StatusCode {
        COMPLETE,
        ERROR;
    }

    /**
     * Mapped Diagnostic Context parameter names.
     *
     * <p>
     * Note that MdcContext.MDC_START_TIME is used for audit messages, and indicates the start of a transaction.
     * Messages in the metrics log record sub-operations of a transaction and thus use different timestamps.
     */
    public enum MdcParameter {
        REQUEST_ID(MdcContext.MDC_REQUEST_ID),
        CLASS_NAME("ClassName"),
        BEGIN_TIMESTAMP("BeginTimestamp"),
        END_TIMESTAMP("EndTimestamp"),
        ELAPSED_TIME("ElapsedTime"),
        STATUS_CODE("StatusCode"),
        RESPONSE_CODE("ResponseCode"),
        RESPONSE_DESCRIPTION("ResponseDescription"),
        TARGET_ENTITY("TargetEntity"),
        TARGET_SERVICE_NAME("TargetServiceName"),
        USER("User");

        private final String parameterName;

        MdcParameter(String parameterName) {
            this.parameterName = parameterName;
        }

        /**
         * Get the MDC logging context parameter name as referenced by the logback configuration
         *
         * @return the MDC parameter name
         */
        public String value() {
            return parameterName;
        }
    }

    /** Our externally advertised service API */
    private static final String SERVICE_NAME_VALUE = "AAI-BAS";

    private static final EELFLogger errorLogger = EELFManager.getInstance().getErrorLogger();
    private static final EELFLogger debugLogger = EELFManager.getInstance().getDebugLogger();
    private static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    private static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    /** Formatting for timestamps logged as Strings (from the MDC) */
    private DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // Records the elapsed time (since the start of servicing a request) for audit logging
    private StopWatch auditStopwatch;

    /**
     * Initialises the MDC (logging context) with default values, to support any logging of messages BEFORE the
     * startAudit() method is invoked.
     */
    private LogHelper() {
        setContextValue(MDC_SERVICE_NAME, SERVICE_NAME_VALUE);
        // This value is not expected to be used in the default logging configuration
        setContextValue(MdcContext.MDC_START_TIME, timestampFormat.format(new Date()));
    }

    /**
     * Begin recording transaction information for a new request. This data is intended for logging purposes. This
     * method does not actually write any messages to the log(s).
     *
     * <p>
     * Initialise the MDC logging context for auditing and metrics, using the HTTP request headers. This information
     * includes: the correlation ID, local application service name/ID, calling host details and authentication data.
     *
     * <p>
     * The request object is used to find the client details (e.g. IP address)
     *
     * @param headers raw HTTP headers
     * @param servletRequest the request
     */
    public void startAudit(final HttpHeaders headers, ServletRequest servletRequest) {
        auditStopwatch = new StopWatch();
        auditStopwatch.start();

        Optional<String> requestId = Optional.empty();
        String serviceInstanceId = null;
        Optional<String> partnerName = Optional.empty();

        if (headers != null) {
            RequestHeaders requestHeaders = new RequestHeaders(headers);
            requestId = Optional.ofNullable(requestHeaders.getCorrelationId());
            serviceInstanceId = requestHeaders.getInstanceId();
            partnerName = Optional.ofNullable(headers.getHeaderString(Headers.FROM_APP_ID));
        }

        String clientHost = null;
        String clientIPAddress = null;
        String user = "<UNKNOWN_USER>";

        if (servletRequest != null) {
            clientHost = servletRequest.getRemoteHost();
            clientIPAddress = servletRequest.getRemoteAddr();

            if (!partnerName.isPresent()) {
                partnerName = Optional.ofNullable(clientHost);
            }
        }

        // Populate standard MDC keys - note that the initialize method calls MDC.clear()
        MdcContext.initialize(requestId.orElse("missing-request-id"), SERVICE_NAME_VALUE, serviceInstanceId,
                partnerName.orElse("<UNKNOWN_PARTNER>"), clientIPAddress);

        setContextValue(MdcParameter.USER, user);
        setContextValue(MdcContext.MDC_REMOTE_HOST, clientHost);
    }

    /**
     * Store a value in the current thread's logging context.
     *
     * @param key non-null parameter name
     * @param value the value to store against the key
     */
    public void setContextValue(String key, String value) {
        debug(key + "=" + value);
        MDC.put(key, value);
    }

    /**
     * Store a value in the current thread's logging context.
     *
     * @param param identifier of the context parameter
     * @param value the value to store for this parameter
     */
    public void setContextValue(MdcParameter param, String value) {
        setContextValue(param.value(), value);
    }

    /**
     * Set a value in the current thread's logging context, only if this is not already set.
     *
     * @param key non-null parameter name
     * @param value the value to store against the key (only if the current value is null)
     */
    public void setDefaultContextValue(String key, String value) {
        if (MDC.get(key) == null) {
            setContextValue(key, value);
        }
    }

    /**
     * Set a value in the current thread's logging context, only if this is not already set.
     *
     * @param param identifier of the context parameter
     * @param value the value to store for this parameter (only if the current value is null)
     */
    public void setDefaultContextValue(MdcParameter param, String value) {
        setContextValue(param.value(), value);
    }

    /** Clear all logging context values. This should be called at start-up only. */
    public void clearContext() {
        debug("Clearing MDC context");
        MDC.clear();
    }

    /**
     * Log an audit message to the audit logger. This method is expected to be called when a response is returned to the
     * caller and/or when the processing of the request completes.
     *
     * @param status status of the service request: COMPLETE/ERROR
     * @param responseCode optional application error code
     * @param responseDescription human-readable description of the response code
     * @param args the argument(s) required to populate the Audit Message log content
     */
    public void logAudit(StatusCode status, String responseCode, String responseDescription, final String... args) {
        if (auditStopwatch == null) {
            debug("Unexpected program state: audit stopwatch not started");
            auditStopwatch = new StopWatch();
            auditStopwatch.start();
        }

        if (auditLogger.isInfoEnabled()) {
            setMdcElapsedTime(auditStopwatch);
            setContextValue(MdcParameter.STATUS_CODE, status.toString());
            setContextValue(MdcParameter.RESPONSE_CODE, responseCode);
            setContextValue(MdcParameter.RESPONSE_DESCRIPTION, responseDescription);
            invokeLogger(auditLogger::info, ApplicationMsgs.MESSAGE_AUDIT, args);
        }
    }

    /**
     * Log an audit message to the audit logger representing a non-specific processing success message.
     *
     * @param msg
     */
    public void logAuditSuccess(String msg) {
        Status status = Status.OK;
        logAudit(StatusCode.COMPLETE, Integer.toString(status.getStatusCode()), status.getReasonPhrase(), msg);
    }

    /**
     * Log an audit message to the audit logger representing an internal error (e.g. for an exception thrown by the
     * implementation). This method is expected to be called when a generic error response is returned to the caller to
     * indicate a processing failure.
     *
     * @param e Exception
     */
    public void logAuditError(Exception e) {
        Status status = Status.INTERNAL_SERVER_ERROR;
        logAudit(StatusCode.ERROR, Integer.toString(status.getStatusCode()), status.getReasonPhrase(), e.getMessage());
    }

    /**
     * Log a message to the metrics log.
     *
     * @param error the log code
     * @param args the info messages
     */
    public void logMetrics(final String... args) {
        if (metricsLogger.isInfoEnabled()) {
            invokeLogger(metricsLogger::info, ApplicationMsgs.MESSAGE_METRIC, args);
        }
    }

    /**
     * @param stopwatch
     * @param args
     */
    public void logMetrics(final StopWatch stopwatch, String... args) {
        setMdcElapsedTime(stopwatch);
        logMetrics(args);
    }

    @Override
    public String formatMsg(@SuppressWarnings("rawtypes") Enum arg0, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebugEnabled() {
        return debugLogger != null && debugLogger.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return errorLogger.isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return errorLogger.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return debugLogger.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return errorLogger.isWarnEnabled();
    }

    /**
     * Log a DEBUG level message to the debug logger.
     *
     * @param message the debug message
     */
    @Override
    public void debug(String message) {
        if (isDebugEnabled()) {
            invokeLogger(debugLogger::debug, message);
        }
    }

    @Override
    public void debug(@SuppressWarnings("rawtypes") Enum errorCode, String... args) {
        if (isDebugEnabled()) {
            invokeErrorCodeLogger(debugLogger::debug, (EELFResolvableErrorEnum) errorCode, args);
        }
    }

    @Override
    public void debug(@SuppressWarnings("rawtypes") Enum errorCode, LogFields arg1, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(@SuppressWarnings("rawtypes") Enum errorCode, String... args) {
        if (isErrorEnabled()) {
            invokeErrorCodeLogger(errorLogger::error, (EELFResolvableErrorEnum) errorCode, args);
        }
    }

    @Override
    public void error(@SuppressWarnings("rawtypes") Enum errorCode, Throwable t, String... args) {
        if (isErrorEnabled()) {
            invokeErrorCodeLogger(errorLogger::error, (EELFResolvableErrorEnum) errorCode, t, args);
        }
    }

    @Override
    public void error(@SuppressWarnings("rawtypes") Enum errorCode, LogFields arg1, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(@SuppressWarnings("rawtypes") Enum errorCode, LogFields arg1, Throwable arg2, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(@SuppressWarnings("rawtypes") Enum errorCode, String... args) {
        if (isInfoEnabled()) {
            invokeErrorCodeLogger(errorLogger::info, (EELFResolvableErrorEnum) errorCode, args);
        }
    }

    @Override
    public void info(@SuppressWarnings("rawtypes") Enum arg0, LogFields arg1, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void info(@SuppressWarnings("rawtypes") Enum arg0, LogFields arg1, MdcOverride arg2, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trace(@SuppressWarnings("rawtypes") Enum errorCode, String... args) {
        if (isTraceEnabled()) {
            invokeErrorCodeLogger(debugLogger::trace, (EELFResolvableErrorEnum) errorCode, args);
        }
    }

    @Override
    public void trace(@SuppressWarnings("rawtypes") Enum arg0, LogFields arg1, String... args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void warn(@SuppressWarnings("rawtypes") Enum errorCode, String... args) {
        if (isWarnEnabled()) {
            invokeErrorCodeLogger(errorLogger::warn, (EELFResolvableErrorEnum) errorCode, args);
        }
    }

    @Override
    public void warn(@SuppressWarnings("rawtypes") Enum arg0, LogFields arg1, String... args) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the method name for a calling method (from the current stack trace)
     *
     * @param level number of levels for the caller (not including this method)
     * @return the class and name of the calling method in the form "class#method"
     */
    public static String getCallerMethodName(int level) {
        StackTraceElement callingMethod = Thread.currentThread().getStackTrace()[level + 2];
        return callingMethod.getClassName() + "#" + callingMethod.getMethodName();
    }

    /**
     * Convenience method to be used only for testing purposes.
     *
     * @return the directory storing the log files
     */
    public static String getLogDirectory() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.att.eelf");
        AsyncAppender appender = (AsyncAppender) logger.getAppender("asyncEELF");
        FileAppender<?> fileAppender = (FileAppender<?>) appender.getAppender("EELF");
        return new File(fileAppender.getFile()).getParent();
    }

    private void setMdcClassName() {
        MDC.put(MdcParameter.CLASS_NAME.value(), getCallerMethodName(3));
    }

    private void unsetMdcClassName() {
        MDC.put(MdcParameter.CLASS_NAME.value(), null);
    }

    /**
     * Set the Begin, End, and Elapsed time values in the MDC context.
     *
     * @param stopwatch
     */
    private void setMdcElapsedTime(final StopWatch stopwatch) {
        long startTime = stopwatch.getStartTime();
        long elapsedTime = stopwatch.getTime();

        setContextValue(MdcParameter.BEGIN_TIMESTAMP, timestampFormat.format(startTime));
        setContextValue(MdcParameter.END_TIMESTAMP, timestampFormat.format(startTime + elapsedTime));
        setContextValue(MdcParameter.ELAPSED_TIME, Long.toString(elapsedTime)); // Milliseconds
    }

    /**
     * @param logMethod the logger method to invoke
     * @param message
     */
    private void invokeLogger(Consumer<String> logMethod, String message) {
        setMdcClassName();
        logMethod.accept(message);
        unsetMdcClassName();
    }

    /**
     * @param logMethod
     * @param msg
     * @param args
     */
    private void invokeLogger(BiConsumer<ApplicationMsgs, String[]> logMethod, ApplicationMsgs msg, String[] args) {
        setMdcClassName();
        logMethod.accept(msg, args);
        unsetMdcClassName();
    }

    /**
     * @param logMethod
     * @param errorEnum
     * @param args
     */
    private void invokeErrorCodeLogger(BiConsumer<EELFResolvableErrorEnum, String[]> logMethod,
            EELFResolvableErrorEnum errorEnum, String[] args) {
        setMdcClassName();
        logMethod.accept(errorEnum, args);
        unsetMdcClassName();
    }

    /**
     * @param logMethod
     * @param errorEnum
     * @param t a Throwable
     * @param args
     */
    private void invokeErrorCodeLogger(TriConsumer<EELFResolvableErrorEnum, Throwable, String[]> logMethod,
            EELFResolvableErrorEnum errorEnum, Throwable t, String[] args) {
        setMdcClassName();
        logMethod.accept(errorEnum, t, args);
        unsetMdcClassName();
    }
}
