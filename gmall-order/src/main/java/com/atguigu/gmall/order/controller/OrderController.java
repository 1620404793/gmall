package com.atguigu.gmall.order.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/confirm")
    public Resp<OrderConfirmVO> confirm(){
        //不需要参数，可以从选中的购物车里直接选
        OrderConfirmVO orderConfirmVO=this.orderService.confirm();
        return Resp.ok(orderConfirmVO);
    }
    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo submitVo){
        this.orderService.submit(submitVo);
        return Resp.ok(null);
    }
}
