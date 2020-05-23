# gmall
购物商城电商平台

#Nacos服务:阿里官方提供了nacos-server

注册中心

    1.	引入依赖：discovery-start
    2.	Application.yml：spring.cloud.nacos.discovery.server.addr=地址
    3.	注解@EnableDiscoveryClient
配置中心

    1.	引入依赖：config -start
    2.	Bootstrap.yml:spring.cloud. nacos.config.server.addr=地址
            spring.cloud. nacos.config.namespace=唯一标志uuid
            spring.cloud. nacos.config.group =组名
            spring.cloud. nacos.ext-config[0].data-id =配置名 //额外配置
            spring.cloud. nacos.ext-config[0].group =组名
            spring.cloud. nacos.ext-config[0].refresh=true //动态刷新

3.注解 @RefreshScope //动态刷新

好处：

    1.可以动态刷新配置，即使改变了配置，也不需要重启
    2.统一管理配置文件
    3.配置版本管理
    
#Gateway：网关组件 （zuul）

	动态路由、负载均衡、身份认证、限流、路径重写、熔断降级、请求过滤
	Spring.cloud.gateway.routes[0]
    1.	Id：唯一标志
    2.	Uri：
    3.	Predicates：断言（判断）
    4.	Filters：过滤器（拦截）
    
    
#商品信息 
Spu：标准商品单元，商品的集合

Sku：库存量单元，具体的商品

规格参数

表关系

保存商品（pms sms wms）

#搜索技术

倒排索引，文档列表，倒排索引区

全文检索：从海量数据中快速获取需要的信息

Lucene:底层api

搜索产品：solr  elasticsearch

Elasticsearch:

	安装：jvm.options elasticsearch
	Kibana
	Ik分词器：ik_max_word  ik_smart
		扩展词典*（nginx配置，添加分词后）

#Rabbitmq

MQ:message queue

作用：解耦、异步、削峰

实现：

    AMQP(rabbitmq 协议 五种消息类型 任何语言都可以实现)  
    JMS（activemq  java规范 提供了两种消息模型 必须是java实现）

#三级分类的查询 

添加缓存：

	标准：1.写的频率低	2.读的频率高

过程：

	1.查询缓存有没有
	2.缓存中没有查询数据库
	3.放入缓存
缓存存在的问题

	雪崩：给缓存的过期时间添加随机值
	穿透：即使数据库中的数据为null,也缓存
	击穿：分布式锁
实现分布式锁

	标准：
		1.排他：
		2.防止死锁，设置有效时间
		3.防止释放别人的锁
	实现：
		1.获取锁（原子性）

商品的详情页：大量的远程调用

优化：页面静态化、缓存、异步编排

异步编排：CompletableFuture  -> 优化大量的远程调用

#注册功能：

	1.校验数据是否可用：用户名 手机号 邮箱
	2.发送短信验证码：生成验证码  发消息并把短信保存到redis中
	3.用户注册功能（新增用户）
		1.校验验证码
		2.生成盐
		3.对密码加盐加密
		4.保存用户信息
		5.删除redis中的验证码

	4.根据用户名和密码查询用户
		1.根据用户名查询用户信息（因为有盐，所以只能用用户查询）
		2.判断用户是否存在
		3.对用户输入的密码加盐加密
		4.和数据库中的密码比较
		
#单点登录：

	无状态登录（jwt 在服务器中不需要保存用户状态）
	有状态登录(session redis)
	Jwt+rsa
	Jwt:
		头部信息：token类型 ：编码方式
		载荷信息：用户具体信息
		签名信息：检验前两部分的信息是否合法 rsa加密
	加密方式：
		对称加密：base64算法简单，效率高
		不可逆加密：md5
		非对称加密：rsa（公钥、私钥
		
#购物车需求

    删除购物车
    查询购物车
    修改数量
    勾选购物车
    比价
    技术选型：
	未登录情况下
		Redis
        Mysql
        Cookies
        IndexDB
        webSQL
        localStorage
    mongodb(NoSQL,硬盘，写比较频繁)
	登陆情况下
		Mysql+redis
		Redis
		Mongodb

#订单

	订单确认页
	 数据模型:orderToken防止表单的重复提交 收货地址列表 配送方式 送货清单 积分
	 IdWorker:雪花算法
	提交订单
	 提交数据模型:orderToken 收货地址 配送方式 支付方式 送货清单 发票 积分 总价
	 业务流程
	 	1.防重
	 	2.验价
	 	3.验库存并锁库存
	 	4.下单
		5.删除购物车
	定时关单：
		1.定时任务（juc (@Scheduled  @Enable Scheduled )）
		2.延时队列(延时队列：设置消息的有效时间/设置消息的死信路由/设置死信			rountingKey，死信队列绑定死信路由，消费者监听私信队列)
支付

	内网穿透：花生壳 哲希云
	阿里沙箱
	支付成功异步回调
秒杀

	页面静态化、限流、异步缓存
	页面限流
	Nginx限流：漏斗算法 令牌桶算法  进入tomcat之前
	网关限流：限流过滤器
	服务器内部限流：信号量
	用户查看订单时：使用闭锁
