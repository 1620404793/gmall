package com.atguigu.gmall.oms.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitMqConfig {
    @Bean("ORDER-TTL-QUEUE")
    public Queue ttlQueue() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-dead-letter-exchange", "GMALL-ORDER-EXCHANGE");
        map.put("x-dead-letter-routing-key", "order.dead");
        map.put("x-message-ttl", 300000);
       // map.put("x-queue-type", "classic");
        return new Queue("ORDER-TTL-QUEUE", true, false, false, map);
    }
    //延时队列和交换机的绑定
    @Bean("ORDER-TTL-BINDING")
    public Binding ttlBuilder() {                              //类型
        return new Binding("ORDER-TTL-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "order.ttl", null);
    }


    @Bean("ORDER-DEAD-QUEUE")
    public Queue dlQueue() {
        return new Queue("ORDER-DEAD-QUEUE", true, false, false, null);
    }
    //死信队列和交换机的绑定
    @Bean("ORDER-DEAD-BINDING")
    public Binding deadBuilder() {
        return new Binding("ORDER-DEAD-QUEUE", Binding.DestinationType.QUEUE, "GMALL-ORDER-EXCHANGE", "order.dead", null);
    }
}
