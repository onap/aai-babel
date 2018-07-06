/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 Nokia Networks Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.babel.xml.generator.model;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Base64;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;

public class TestGeneratorUtil {

    private static final byte[] TEST_BYTES = "TestBytes".getBytes();
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Test
    public void shouldEncodeUsingBase64() {
        byte[] expected = Base64.getEncoder().encode(TEST_BYTES);

        byte[] result = GeneratorUtil.encode(TEST_BYTES);

        assertThat(result, is(expected));
    }

    @Test
    public void shouldReturnEmptyByteArrayWhenNullPassedToEncode() {
        byte[] result = GeneratorUtil.encode(null);

        assertThat(result, is(EMPTY_BYTE_ARRAY));
    }

    @Test
    public void shouldDecodeUsingBase64() {
        byte[] input = Base64.getEncoder().encode(TEST_BYTES);
        byte[] expected = Base64.getDecoder().decode(input);

        byte[] result = GeneratorUtil.decode(input);

        assertThat(result, is(expected));
    }

    @Test
    public void shouldReturnEmptyByteArrayWhenNullPassedToDecode() {
        byte[] result = GeneratorUtil.decode(null);

        assertThat(result, is(EMPTY_BYTE_ARRAY));
    }

    @Test
    public void shouldReturnNullWhenNullPassedToCheckSum() {
        assertNull(GeneratorUtil.checkSum(null));
    }

    @Test
    public void shouldReturnSameCheckSumForIdenticalInput() {
        String checkSum1 = GeneratorUtil.checkSum(TEST_BYTES);
        String checkSum2 = GeneratorUtil.checkSum(TEST_BYTES);

        assertThat(checkSum1, is(checkSum2));
    }
}
