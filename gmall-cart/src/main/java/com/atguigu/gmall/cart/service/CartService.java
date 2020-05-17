package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    private static final String KEY_PREFIX="gmall:cart:";
    private static final String PRICE_PREFIX="gmall:sku:";
    @Autowired
    private StringRedisTemplate redisTemplate;
    public void addCarts(Cart cart) {
        String key = getLoginState();

        //获取购物车 ,获取的是用户的 hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //判断购物车中是否有该纪录  操作redos必须使用string类型的数据，否则永远判断不出来
        String skuId = cart.getSkuId().toString();//一定带toString
        Integer count = cart.getCount();//前台cart
        if (hashOps.hasKey(skuId)){
            //有，则更新记录
            //获取购物车中的cart纪录
            String cartJson = hashOps.get(skuId).toString();
            //序列化，更新数量
            cart=JSON.parseObject(cartJson,Cart.class);//购物车中cart
            cart.setCount(cart.getCount()+count);
            //重新写入redis中的购物车中
              //重复了，方if外面
            //hashOps.put(skuId,JSON.toJSONString(cart));
        }else{
            //没有，则新加纪录
            cart.setCheck(true);
            //查询sku相关的信息
            SkuInfoEntity skuInfoEntity = gmallPmsClient.querySkuById(cart.getSkuId()).getData();
            if (skuInfoEntity==null){
                return;
            }
            cart.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
            cart.setTitle(skuInfoEntity.getSkuTitle());
            cart.setPrice(skuInfoEntity.getPrice());
            //查询营销属性
            List<SkuSaleAttrValueEntity> attrValueEntities = gmallPmsClient.querySkuSaleAttrValueBySkuId(cart.getSkuId()).getData();
            cart.setSaleAttrValues(attrValueEntities);
            //查询营销信息
            List<SkuSaleVO> skuSaleVOS = this.gmallSmsClient.querySkuSalesBySkuId(cart.getSkuId()).getData();
            cart.setSales(skuSaleVOS);
            //查询库存信息
            List<WareSkuEntity> wareSkuEntities = gmallWmsClient.queryWareSkuBySkuId(cart.getSkuId()).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()>0));
            }
            //新增购物车时，额外保存一份当前价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX+skuId,skuInfoEntity.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    private String getLoginState() {
        String key=KEY_PREFIX;

        //获取登录状态
        UserInfo userInfo= LoginInterceptor.getUserInfo();
        if (userInfo.getId()!=null){
            key+=userInfo.getId();
        }else {
            key+=userInfo.getUserKey();
        }
        return key;
    }

    public List<Cart> queryCarts() {
        //获取登录状态
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //查询未登录的购物车
        String unLoginKey = KEY_PREFIX+userInfo.getUserKey();
        List<Cart> unLoginCarts=null;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> cartJsonList = unLoginHashOps.values();
        if (!CollectionUtils.isEmpty(cartJsonList)){
            unLoginCarts= cartJsonList.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //查询当前价格
                String priceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                if(priceString!=null){
                    cart.setCurrentPrice(new BigDecimal(priceString));
                }
                return cart;
            }
        ).collect(Collectors.toList());
        }
        //判断是否登陆，未登录，直接返回
        if (userInfo.getId()==null){
            return unLoginCarts;
        }
        //登录，购物车同步
        String loginKey=KEY_PREFIX+userInfo.getId();
        BoundHashOperations<String, Object, Object> loginHashOps = redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                Integer count = cart.getCount(); //购物车里的一件具体产品sku
                if (loginHashOps.hasKey(skuId)){//必须使用字符串格式
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart=JSON.parseObject(cartJson,Cart.class);
                    cart.setCount(count+cart.getCount());
                }
                loginHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
            //同步完成后，需要删除未登录状态下的购物车
            this.redisTemplate.delete(unLoginKey);
        }
        //查询登录状态的购物车
        List<Object> loginCartJsonList = loginHashOps.values();
        List<Cart> loginCarts = loginCartJsonList.stream().map(cartJson ->{
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            //查询当前价格
            String priceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
            cart.setCurrentPrice(new BigDecimal(priceString));
            return cart;
        } ).collect(Collectors.toList());
        return loginCarts;
    }

    public void updateCart(Cart cart) {
        String key = this.getLoginState();

        //获取购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        Integer count = cart.getCount();
        //判断更新的这条纪录，在购物车中是否存在
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    public void deleteCart(Long skuId) {
        String key = this.getLoginState();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
        }
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartJsons = hashOps.values();
        List<Cart> carts = cartJsons.stream().map(cartJson ->
                JSON.parseObject(cartJson.toString(), Cart.class)
        ).filter(Cart::getCheck).collect(Collectors.toList());
        return carts;
    }
}
