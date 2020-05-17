package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
    private static final String pubKeyPath = "E:\\ideaProject\\easyMall\\rsa\\rsa.pub";

    private static final String priKeyPath = "E:\\ideaProject\\easyMall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);//设置过期时间
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1ODkxNjgwNDV9.fm8SuXoir_asuMp7zcvY2YTTB6DgX7DcaffwBWDBbzLROLtaQbjCafEPx-Kt3WsqwLDUFQUSCor1yTEYKoTy_EwJrhSGqmuFGzNVDFpwkuzJbVYnIblHvgD90nPWKkLIK_Kaejnyiy45aU8VLp2ptjP2G8VEG1vQBy_4RG2VvsqMt8kdh0Hi52jqHy-4mOY-LtXJY0vTH-XvkC3SuqpuIFpAJt99FBXjcewndPa2FQhzmUcbftkT44qbibk42Mlg6ZdtUSzV8ki-L4jUGGixAgsWraHrSL7OgRy60Ky2yYgot-JFT4XY8XSljkTfvm3zlVhd8CHhpX0_EN-aP0KYtw";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
