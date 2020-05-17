package com.atguigu.gmall.order.interceptors;

import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;

import com.atguigu.gmall.order.config.JwtPropreties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
//@Scope("pototype") 防止线程不安全
@EnableConfigurationProperties(JwtPropreties.class)
public class LoginInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JwtPropreties jwtPropreties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL=new ThreadLocal<>();

    public static UserInfo USER_INFO=null;//如果有这种状态字段的话，需要给这个bean对象注释成多例模式
    /**
     * 统一获取登录状态
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo=new UserInfo();
        //获取cookie中的token信息（jwt) 及userKey信息
        String token = CookieUtils.getCookieValue(request, jwtPropreties.getCookieName());


        //判断有没有token
        if (StringUtils.isNotBlank(token)){
            //解析token的信息
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, this.jwtPropreties.getPublicKey());

            userInfo.setId(new Long(infoFromToken.get("id").toString()));//不能强转
        }

        THREAD_LOCAL.set(userInfo);//key指当前线程对象，只设置值就可以了
        //USER_INFO=userInfo; 为了线程安全，这里使用request
       // request.setAttribute("userKey",userKey);
        //request.setAttribute("id",userInfo.getId());
        return super.preHandle(request, response, handler);
    }

    public static UserInfo getUserInfo(){
        UserInfo userInfo = THREAD_LOCAL.get();
        return userInfo;
    }

    /**
     * 必须手动清楚threadLocal
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //必须手动清楚threadLocal,因为这里使用的是tomcat的线程池
        THREAD_LOCAL.remove();
    }
}
