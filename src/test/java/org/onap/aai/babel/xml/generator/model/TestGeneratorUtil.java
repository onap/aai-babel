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

import java.util.Base64;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class TestGeneratorUtil {

    @Test
    public void shouldEncodeUsingBase64() {
        byte[] input = "TestBytes".getBytes();
        byte[] expected = Base64.getEncoder().encode(input);

        byte[] result = GeneratorUtil.encode(input);

        assertThat(result, is(expected));
    }

    @Test
    public void shouldReturnEmptyByteArrayWhenNullPassedToEncode() {
        byte[] result = GeneratorUtil.encode(null);

        assertThat(result, is(new byte[0]));
    }

    @Test
    public void shouldDecodeUsingBase64() {
        byte[] input = Base64.getEncoder().encode("TestBytes".getBytes());
        byte[] expected = Base64.getDecoder().decode(input);

        byte[] result = GeneratorUtil.decode(input);

        assertThat(result, is(expected));
    }

    @Test
    public void shouldReturnEmptyByteArrayWhenNullPassedToDecode() {
        byte[] result = GeneratorUtil.decode(null);

        assertThat(result, is(new byte[0]));
    }

    @Test
    public void shouldReturnNullWhenNullPassedToCheckSum() {
        assertNull(GeneratorUtil.checkSum(null));
    }

    @Test
    public void shouldReturnSameCheckSumForIdenticalInput() {
        byte[] input = "InputToCheckSum".getBytes();

        String checkSum1 = GeneratorUtil.checkSum(input);
        String checkSum2 = GeneratorUtil.checkSum(input);

        assertThat(checkSum1, is(checkSum2));
    }
}
