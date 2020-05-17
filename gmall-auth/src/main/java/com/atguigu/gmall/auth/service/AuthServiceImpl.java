package com.atguigu.gmall.auth.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.config.JwtPropreties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.ums.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties(JwtPropreties.class)//只需启用一次就可以了，因为spring容器是单例的
public class AuthServiceImpl implements IAuthService {
    @Autowired
    private JwtPropreties jwtPropreties;
    @Autowired
    private GmallUmsClient gmallUmsClient;

    public String accredit(String username, String password) {
        //远程调用，校验用户名和密码
        Resp<MemberEntity> memberEntityResp = this.gmallUmsClient.queryUser(username, password);
        MemberEntity memberEntity = memberEntityResp.getData();
        //判断用户是否为null
        if (memberEntity == null) {
            return null;
        }
        try {
            //制作jwt
            Map<String, Object> map = new HashMap<>();
            map.put("id", memberEntity.getId());
            map.put("username", memberEntity.getUsername());
            //jwt和token的关系    token是jwt工具生成的一种
            String token = JwtUtils.generateToken(map, this.jwtPropreties.getPrivateKey(), this.jwtPropreties.getExpire());
            return token;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
