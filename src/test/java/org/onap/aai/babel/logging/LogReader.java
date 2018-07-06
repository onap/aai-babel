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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;

public class LogReader {

    private Map<String, Path> cachedLogMap = new HashMap<>();
    private Map<String, BufferedReader> readersMap = new HashMap<>();
    private BufferedReader cachedReader;

    public LogReader(String logDirectory, String logFilePrefix) throws IOException {
        cachedReader = getReader(logDirectory, logFilePrefix);
    }

    private BufferedReader getReader(String logDirectory, String logFilePrefix) throws IOException {
        BufferedReader reader = readersMap.get(logFilePrefix);
        if (reader == null) {
            reader = new BufferedReader(new FileReader(getLogFile(logDirectory, logFilePrefix)));
            while (reader.readLine() != null) {
                // Consume all lines
            }
            readersMap.put(logFilePrefix, reader);
        }
        return reader;
    }

    /**
     * @param logDirectory
     * @return the most recently created log file.
     * @throws IOException
     */
    public File getLogFile(String logDirectory, String filenamePrefix) throws IOException {
        Path cachedLog = cachedLogMap.get(filenamePrefix);

        if (cachedLog == null) {
            Optional<Path> latestFilePath = Files.list(Paths.get(logDirectory))
                    .filter(f -> Files.isDirectory(f) == false && f.getFileName().toString().startsWith(filenamePrefix))
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
            if (latestFilePath.isPresent()) {
                cachedLog = latestFilePath.get();
            } else {
                throw new IOException("No validation log files were found!");
            }
        }
        return cachedLog.toFile();
    }

    /**
     * Wait for and read new log entries.
     *
     * @return new lines appended to the log file
     * @throws IOException If an I/O error occurs
     */
    public String getNewLines() throws IOException {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();

        while (!cachedReader.ready()) {
            if (stopwatch.getTime() > TimeUnit.SECONDS.toMillis(30)) {
                Assert.fail("Test took too long");
            }
            // else keep waiting
        }

        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = cachedReader.readLine()) != null) {
            lines.append(line).append(System.lineSeparator());
        }
        return lines.toString();
    }
}
