package com.atguigu.gmall.wms.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitMqConfig {
    @Bean("WMS-TTL-QUEUE")
    public Queue ttlQueue() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-dead-letter-exchange", "GMALL-ORDER-EXCHANGE");
        map.put("x-dead-letter-routing-key", "stock.unlock");
        map.put("x-message-ttl", 350000);
       // map.put("x-queue-type", "classic");
        return new Queue("WMS-TTL-QUEUE", true, false, false, map);
    }
    //延时队列和交换机的绑定
    @Bean("WMS-TTL-BINDING")
    public Binding ttlBuilder() {                              //类型
        return new Binding("WMS-TTL-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "stock.ttl", null);
    }


   /* @Bean("WMS-DEAD-QUEUE")
    public Queue dlQueue() {
        return new Queue("WMS-DEAD-QUEUE", true, false, false, null);
    }
    //死信队列和交换机的绑定
    @Bean("WMS-DEAD-BINDING")
    public Binding deadBuilder() {
        return new Binding("WMS-DEAD-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "stock.dead", null);
    }*/
}
