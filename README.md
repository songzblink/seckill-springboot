https://blog.csdn.net/unique_perfect/article/details/109439039
https://www.bilibili.com/video/BV13a4y1t7Wh?t=10&p=3
## 系统场景
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

## 防止超卖
用户体验不好最多是让用户没有参与到活动，但要是卖多了问题就严重了起来，所以首先应该解决的问题是防止超卖。

### 数据库表
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

### 创建快速SpringBoot项目

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

### 分析业务

![image.png](https://pic.tyzhang.top/images/2021/04/17/image.png)

### 开发代码

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

### 正常测试

在正常测试下不会出现超卖问题。