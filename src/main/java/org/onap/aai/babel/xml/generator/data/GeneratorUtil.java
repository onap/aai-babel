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

import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/** Utility method class for artifact generation. */
public class GeneratorUtil {

    /*
     * Private constructor to prevent instantiation
     */
    private GeneratorUtil() {
        throw new UnsupportedOperationException("This static class should not be instantiated!");
    }

    /**
     * Decodes Base64 encode byte array input.
     *
     * @param input Base64 encoded byte array
     * @return Decoded byte array
     */
    public static byte[] decoder(byte[] input) {
        return input != null ? Base64.getDecoder().decode(input) : new byte[0];
    }

    /**
     * Encode a byte array input using Base64 encoding.
     *
     * @param input Input byte array to be encoded
     * @return Base64 encoded byte array
     */
    public static byte[] encode(byte[] input) {
        return input != null ? Base64.getEncoder().encode(input) : new byte[0];
    }

    /**
     * Calculate the checksum for a given input.
     *
     * @param input Byte array for which the checksum has to be calculated
     * @return Calculated checksum of the input byte array
     */
    public static String checkSum(byte[] input) {
        return input != null ? DigestUtils.md5Hex(input).toUpperCase() : null;
    }
}
