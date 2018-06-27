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
