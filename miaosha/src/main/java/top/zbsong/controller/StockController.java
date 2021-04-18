package top.zbsong.controller;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zbsong.service.OrderService;
import top.zbsong.service.UserService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {

    @Autowired
    private OrderService orderService;
    // 令牌桶实例
    private RateLimiter rateLimiter = RateLimiter.create(10);

    @Autowired
    private UserService userService;

    /**
     * 秒杀方法  乐观锁防止超卖+令牌桶算法限流+md5接口隐藏+单用户访问频率限制
     *
     * @param id
     * @param userid
     * @param md5
     * @return
     */
    @RequestMapping("killtokenmd5limit")
    public String killtokenmd5limit(Integer id, Integer userid, String md5) {
        System.out.println("秒杀商品的id=" + id);
        // 加入令牌桶限流措施
        // 设置一个等待时间，在等待时间内没有获取到响应token则抛弃
        if (!rateLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            log.info("抛弃请求：抢购失败，当前秒杀活动过于火爆，请重试!");
            return "抢购失败，当前秒杀活动过于火爆，请重试!";
        }
        try {
            // 单用户调用接口的频率限制
            int count = userService.saveUserCount(userid);
            log.info("用户截止该次的访问次数为：[{}]", count);
            // 进行调用次数的判断
            boolean isBanned = userService.getUserCount(userid);
            if (isBanned) {
                log.info("购买失败，超过频率限制!");
                return "购买失败，超过频率限制!";
            }
            // 根据秒杀商品id去调用秒杀业务
            int orderId = orderService.kill(id, userid, md5);
            return "秒杀成功，订单id为" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    /**
     * 根据商品id和用户id获取md5值
     *
     * @param id
     * @param userid
     * @return
     */
    @RequestMapping("md5")
    public String getMd5(Integer id, Integer userid) {
        String md5;
        try {
            md5 = orderService.getMd5(id, userid);
        } catch (Exception e) {
            e.printStackTrace();
            return "获取md5失败：" + e.getMessage();
        }
        return "获取md5信息为：" + md5;
    }

    /**
     * 秒杀方法  乐观锁防止超卖+令牌桶算法限流+md5接口隐藏
     *
     * @param id
     * @param userid
     * @param md5
     * @return
     */
    @RequestMapping("killtokenmd5")
    public String killtoken(Integer id, Integer userid, String md5) {
        System.out.println("秒杀商品的id=" + id);
        // 加入令牌桶限流措施
        // 设置一个等待时间，在等待时间内没有获取到响应token则抛弃
        if (!rateLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            log.info("抛弃请求：抢购失败，当前秒杀活动过于火爆，请重试!");
            return "抢购失败，当前秒杀活动过于火爆，请重试!";
        }
        try {
            int orderId = orderService.kill(id, userid, md5);
            return "秒杀成功，订单id为" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    /**
     * 根据秒杀商品id去调用秒杀业务  使用乐观锁防止超卖
     *
     * @param id
     * @return
     */
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

    /**
     * 根据秒杀商品id去调用秒杀业务  使用乐观锁防止超卖 + 令牌桶算法限流
     *
     * @param id
     * @return
     */
    @GetMapping("killtoken")
    public String killtoken(Integer id) {
        System.out.println("秒杀商品的id=" + id);
        // 加入令牌桶限流措施
        // 设置一个等待时间，在等待时间内没有获取到响应token则抛弃
        if (!rateLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            log.info("抛弃请求：抢购失败，当前秒杀活动过于火爆，请重试!");
            return "抢购失败，当前秒杀活动过于火爆，请重试!";
        }
        try {
            int orderId = orderService.kill(id);
            return "秒杀成功，订单id为" + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

/*  // 令牌桶实例测试
    @GetMapping("sale")
    public String sale(Integer id) {
        // 1.没有获取到token请求一直阻塞，直到获得token令牌
        // log.info("等待的时间：" + rateLimiter.acquire());
        // 2.设置一个等待时间，在等待时间内没有获取到响应token则抛弃
        if (rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            System.out.println("当前请求被限流，直接抛弃，无法调用后续秒杀逻辑");
            return "抢购失败!";
        }
        System.out.println("处理业务.................");
        return "测试令牌桶";
    }
    */
}
