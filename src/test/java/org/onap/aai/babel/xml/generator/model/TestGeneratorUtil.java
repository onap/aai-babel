/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 Nokia Networks Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Assert;
import org.junit.Test;
import org.onap.aai.babel.xml.generator.data.GeneratorUtil;

public class TestGeneratorUtil {

    @Test
    public void encode_encodesUsingBase64() {
        byte[] input = "TestBytes".getBytes();
        byte[] expected = Base64.getEncoder().encode(input);

        byte[] result = GeneratorUtil.encode(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void encode_whenNullPassed_thenReturnsEmptyByteArray() {
        byte[] input = null;
        byte[] expected = new byte[0];

        byte[] result = GeneratorUtil.encode(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void decode_decodesUsingBase64() {
        byte[] input = Base64.getEncoder().encode("TestBytes".getBytes());
        byte[] expected = Base64.getDecoder().decode(input);

        byte[] result = GeneratorUtil.decode(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void decode_whenNullPassed_thenReturnsEmptyByteArray() {
        byte[] input = null;
        byte[] expected = new byte[0];

        byte[] result = GeneratorUtil.decode(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void checkSum_whenNullPassed_thenReturnNull(){
        Assert.assertNull(GeneratorUtil.checkSum(null));
    }

    @Test
    public void checkSum_returnsSameSumForIdenticalInput(){
        byte[] input = "InputToCheckSum".getBytes();

        String checkSum1 = GeneratorUtil.checkSum(input);
        String checkSum2 = GeneratorUtil.checkSum(input);

        Assert.assertEquals(checkSum1, checkSum2);
    }
}
