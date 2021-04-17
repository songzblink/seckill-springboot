https://blog.csdn.net/unique_perfect/article/details/109439039
https://www.bilibili.com/video/BV13a4y1t7Wh?t=10&p=3

## 1.系统场景
针对单系统情况下的一个Demo。


秒杀系统的核心：
- 严格防止超卖：库存 100 件结果卖了 120 件，这应该是最优先保证的
- 防止黑产：原本应该卖给 100 个人，结果全被一个人买走了
- 保证用户体验：高并发下，网页打不开，支付不成功，购物车进不去，地址改不了，涉及到很多技术，这个问题应该是在第一个问题解决了的前提下再考虑

保护措施：
- 乐观锁防止超卖 --- 核心基础
- 令牌桶限流 --- 
- Redis 缓存
- 消息队列异步处理订单
- ……

## 2.防止超卖
用户体验不好最多是让用户没有参与到活动，但要是卖多了问题就严重了起来，所以首先应该解决的问题是防止超卖。

### 2.1 数据库表
```mysql
-- ----------------------------
-- Table structure for stock
-- ----------------------------
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '名称',
  `count` int(11) NOT NULL COMMENT '库存',
  `sale` int(11) NOT NULL COMMENT '已售',
  `version` int(11) NOT NULL COMMENT '乐观锁，版本号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for stock_order
-- ----------------------------
DROP TABLE IF EXISTS `stock_order`;
CREATE TABLE `stock_order` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sid` int(11) NOT NULL COMMENT '库存ID',
  `name` varchar(30) NOT NULL DEFAULT '' COMMENT '商品名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

### 2.2 创建快速SpringBoot项目

#### 依赖

在创建项目时选择添加

- Lombok
- Spring Web
- Mybatis Framework
- MySql Driver

修改 MySql 驱动依赖版本号为系统数据库版本号；指定 lombok 版本号，

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.20</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.10</version>
    <optional>true</optional>
</dependency>
```

mybatis 和数据库整合是还需要一个数据源，添加数据源依赖

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.1.19</version>
</dependency>
```

于是完整的依赖文件 pom.xml 为

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.3.3.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>top.zbsong</groupId>
    <artifactId>miaosha</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>miaosha</name>
    <description>miaosha Demo project for Spring Boot</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.1.4</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.19</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.20</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.10</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 配置文件

application.properties

```properties
# 端口
server.port=80
# 应用的上下文路径，也可以称为项目路径，是构成url地址的一部分
server.servlet.context-path=/ms

# 配置数据源
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/ms
spring.datasource.username=root
spring.datasource.password=123456

# 配置mybatis
mybatis.mapper-locations=classpath:top/zbsong/mapper/*.xml
mybatis.type-aliases-package=top.zbsong.entity

# 日志
logging.level.root=info
logging.level.top.zbsong.dao=debug
```

### 2.3 分析业务

![image.png](https://pic.tyzhang.top/images/2021/04/17/image.png)

### 2.4 开发代码

#### DAO代码

```java
public interface StockDAO {
    // 根据商品id查询库存信息的方法
    Stock chechStock(Integer id);
    // 根据商品id扣除库存
    void updateSale(Stock stock);
}

public interface OrderDAO {
    // 创建订单方法
    void createOrder(Order order);
}
```

#### Service 代码

```java
@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    private StockDAO stockDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Override
    public int kill(Integer id) {
        // 根据商品id去校验库存，检验的都是已售和库存的关系
        Stock stock = stockDAO.chechStock(id);
        // 初始的库存等于已售库存，说明没有了
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        } else {
            // 扣除库存
            stock.setSale(stock.getSale() + 1);
            stockDAO.updateSale(stock);
            // 创建订单
            Order order = new Order();
            order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
            orderDAO.createOrder(order);
            return order.getId();
        }
    }
}
```

#### Controller代码

```java
@RestController
@RequestMapping("stock")
public class StockController {

    @Autowired
    private OrderService orderService;

    //  根据秒杀商品id去调用秒杀业务
    @GetMapping("kill")
    public String kill(Integer id) {
        System.out.println("秒杀商品的id=" + id);
        try {
            int orderId = orderService.kill(id);
            return "秒杀成功，订单id为" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
```

### 2.5 正常测试

在正常测试下不会出现超卖问题。

### 2.6 使用Jmeter进行压力测试

#### 介绍

Apache JMeter 是 Apache 组织开发的基于 Java 的压力测试工具。用于对软件做压力测试，它最初被设计用于 Web 应用测试，但后来扩展到其他测试领域。 它可以用于测试静态和动态资源，例如静态文件、Java 小服务程序、CGI 脚本、Java 对象、数据库、FTP 服务器， 等等。JMeter 可以用于对服务器、网络或对象模拟巨大的负载，来自不同压力类别下测试它们的强度和分析整体性能。另外，JMeter 能够对应用程序做功能/回归测试，通过创建带有断言的脚本来验证你的程序返回了你期望的结果

#### 安装Jmeter

官网：`https://jmeter.apache.org/`

```
# 1.下载jmeter
 	   https://jmeter.apache.org/download_jmeter.cgi
		 下载地址:https://mirror.bit.edu.cn/apache//jmeter/binaries/apache-jmeter-5.2.1.tgz
# 2.解压缩
	backups    ---用来对压力测试进行备份目录
    bin        ---Jmeter核心执行脚本文件
    docs	   ---官方文档和案例
    extras     ---额外的扩展
    lib        ---第三方依赖库
    licenses   ---说明
    printable_docs ---格式化文档

# 3.安装Jmeter
	  0.要求: 必须事先安装jdk环境
	  1.配置jmeter环境变量
	      新建环境变量：JMETER_HOME=G:\apache-jmeter-5.4.1
		  在Path中添加环境变量：Path=G:\apache-jmeter-5.4.1\bin
      2.是配置生效(Linux或者Mac需要)
          source ~/.bash_profile
      3.测试jemeter
      	  在 cmd 中输入 jmeter -v
```

#### 压力测试

`jmeter -n -t [jmx file] -l [results file] -e -o [Path to web report folder]`

- jmx file：jmx压力测试文件
- results file：结果输出的文件
- Path to web report folder：生成html版压力测试报告文件

线程数 2000，库存设置 100，输入以下命令：

`jmeter -n -t ./秒杀测试.jmx -l ./miaosha.txt -e -o ./html`

**测试结果**

![image177987f908983551.png](https://pic.tyzhang.top/images/2021/04/17/image177987f908983551.png)

100 件商品都被秒杀完毕，但是订单却有 775 个，出现了超卖问题，后台也报错。

### 2.7 悲观锁解决商品超卖问题

有人肯定想到了加 synchronized 关键字到 OrderServiceImpl 的 kill 方法上来解决超卖问题。但是**业务方法不能加 synchronized**，需要加在调用处，保证线程执行比事务范围大，否则还是不能解决超卖问题。

```java
// 调用处添加同步代码块
synchronized {
    int orderId = orderService.kill(id);
    return "秒杀成功，订单id为" + String.valueOf(orderId);
}
```

synchronized 线程如果比事务范围小, 释放锁后 ，事务没有结束，数据没有提交到数据库，库存没有改，第二个线程如果执行比较快，跑过了第一个，将数据提交后，第一个才提交事务，这样就可能出现了超卖。所以同步代码块应该加到调用处。

并且悲观锁会让线程排队，效率特别低。

[Synchronized锁在Spring事务管理下，为啥还线程不安全？](https://zhuanlan.zhihu.com/p/165573700)

### 2.8 乐观锁解决商品超卖问题

**说明:** 使用乐观锁解决商品的超卖问题,实际上是把主要防止超卖问题交给数据库解决,利用数据库中定义的 `version字段` 以及数据库中的 `事务` 实现在并发情况下商品的超卖问题。(CAS)

#### 修改代码

**校验库存的方法(不变)**

```java
private Stock checkStock(Integer id) {
    Stock stock = stockDAO.checkStock(id);
    if (stock.getSale().equals(stock.getCount())) {
        throw new RuntimeException("库存不足");
    }
    return stock;
}
```

```xml
<!--根据秒杀商品的id去查询库存-->
<select id="chechStock" parameterType="int" resultType="Stock">
    select id,name,count,sale,version from stock
    where id=#{id}
</select>
```

**更新库存方法改造**

```java
private void updateSale(Stock stock) {
    // 在sql层面完成销量 sale+1 和版本号 version+1，并且根据商品id和版本号同时更新的商品。
    int updateRows = stockDAO.updateSale(stock);
    if (updateRows == 0) {
        throw new RuntimeException("抢购失败，请重试!!!");
    }
}
```

```xml
<!--根据商品id去扣除库存-->
<update id="updateSale" parameterType="Stock">
    update stock set
        sale=sale+1,
        version=version+1
    where id=#{id} and version=#{version}
</update>
```

**创建订单（不变）**

```java
// 创建订单
private Integer createOrder(Stock stock) {
    Order order = new Order();
    order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
    orderDAO.createOrder(order);
    return order.getId();
}
```

```xml
<!--创建订单-->
<!--
    useGeneratedKeys="true" 根据数据库主键生成策略来帮我们生成id
    keyProperty="id" 把生成的id放到传到对象的id属性中
        -->
<insert id="createOrder" parameterType="Order" useGeneratedKeys="true" keyProperty="id">
    insert into stock_order values(#{id},#{sid},#{name},#{createDate})
</insert>
```

**完整的业务方法与Mapper.xml**

Service方法

```java
@Override
public int kill(Integer id) {
    // 校验库存
    Stock stock = checkStock(id);
    // 扣除库存
    updateSale(stock);
    // 创建订单
    return createOrder(stock);
}

// 校验库存
private Stock checkStock(Integer id) {
    Stock stock = stockDAO.checkStock(id);
    if (stock.getSale().equals(stock.getCount())) {
        throw new RuntimeException("库存不足");
    }
    return stock;
}

// 扣除库存
private void updateSale(Stock stock) {
    // 在sql层面完成销量 sale+1 和版本号 version+1，并且根据商品id和版本号同时更新的商品。
    int updateRows = stockDAO.updateSale(stock);
    if (updateRows == 0) {
        throw new RuntimeException("抢购失败，请重试!!!");
    }
}

// 创建订单
private Integer createOrder(Stock stock) {
    Order order = new Order();
    order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
    orderDAO.createOrder(order);
    return order.getId();
}
```

StockDAOMapper.xml

```xml
<!--根据秒杀商品的id去查询库存-->
<select id="chechStock" parameterType="int" resultType="Stock">
    select id,name,count,sale,version from stock
    where id=#{id}
</select>
<!--根据商品id去扣除库存-->
<update id="updateSale" parameterType="Stock">
    update stock set
    sale=sale+1,
    version=version+1
    where id=#{id} and version=#{version}
</update>
```

OrderDAOMapper.xml

```xml
<!--创建订单-->
<!--
    useGeneratedKeys="true" 根据数据库主键生成策略来帮我们生成id
    keyProperty="id" 把生成的id放到传到对象的id属性中
        -->
<insert id="createOrder" parameterType="Order" useGeneratedKeys="true" keyProperty="id">
    insert into stock_order values(#{id},#{sid},#{name},#{createDate})
</insert>
```

#### 测试

同样设置库存为 100，并发的线程数量为 2000。

运行 Jmeter 压力测试工具：`jmeter -n -t ./秒杀测试.jmx`

![imagecb13a9628f71bcf6.png](https://pic.tyzhang.top/images/2021/04/17/imagecb13a9628f71bcf6.png)

可以在图中看到，stock 表中 sale 和 version 字段都增加到了 100，stock_order 表也只出现了 100 条订单信息。说明乐观锁成功解决了商品的超卖问题。

但是在大型的互联网项目中，可能同一时间有成千上万的请求同时过来，我们的服务器肯定是承受不住的，所以需要对请求进行一定的限流处理。