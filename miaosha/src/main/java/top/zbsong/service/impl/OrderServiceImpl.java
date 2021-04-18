package top.zbsong.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.zbsong.dao.OrderDAO;
import top.zbsong.dao.StockDAO;
import top.zbsong.dao.UserDAO;
import top.zbsong.entity.Order;
import top.zbsong.entity.Stock;
import top.zbsong.entity.User;
import top.zbsong.service.OrderService;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private StockDAO stockDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public int kill(Integer id) {
        // 校验Redis中的秒杀商品是否超时
        if (!stringRedisTemplate.hasKey("kill" + id)) {
            throw new RuntimeException("当前商品的抢购活动已经结束啦~");
        }
        // 校验库存
        Stock stock = checkStock(id);
        // 扣除库存
        updateSale(stock);
        // 创建订单
        return createOrder(stock);
    }

    /**
     * 根据商品id和用户id生成md5值
     *
     * @param id
     * @param userid
     * @return
     */
    @Override
    public String getMd5(Integer id, Integer userid) {
        // 检验用户的合法性 userid存在用户信息
        User user = userDAO.findById(userid);
        if (user == null) {
            throw new RuntimeException("用户信息不存在!");
        }
        log.info("用户信息：[{}]", user.toString());
        // 检验商品的合法性 id存在商品信息
        Stock stock = stockDAO.checkStock(id);
        if (stock == null) {
            throw new RuntimeException("商品信息不合法!");
        }
        log.info("商品信息：[{}]", stock.toString());
        // 生成hashkey
        String hashkey = "KEY_" + userid + "_" + id;
        // 生成md5，这里的 "!QJS#" 是一个盐，随机生成
        String key = DigestUtils.md5DigestAsHex((userid + id + "!Q*JS#").getBytes());
        stringRedisTemplate.opsForValue().set(hashkey, key, 3600, TimeUnit.SECONDS);
        log.info("Redis写入：[{}] [{}]", hashkey, key);
        return key;
    }

    @Override
    public int kill(Integer id, Integer userid, String md5) {
        // 校验Redis中的秒杀商品是否超时
        if (!stringRedisTemplate.hasKey("kill" + id)) {
            throw new RuntimeException("当前商品的抢购活动已经结束啦~");
        }

        // 验证签名
        String hashKey = "KEY_" + userid + "_" + id;
        String s = stringRedisTemplate.opsForValue().get(hashKey);
        if (s == null) {
            throw new RuntimeException("没有携带验证签名，请求不合法!");
        }
        if (!s.equals(md5)) {
            throw new RuntimeException("当前请求数据不合法，清稍后再试!");
        }
        // 校验库存
        Stock stock = checkStock(id);
        // 扣除库存
        updateSale(stock);
        // 创建订单
        return createOrder(stock);
    }

    /**
     * 校验库存
     *
     * @param id
     * @return
     */
    private Stock checkStock(Integer id) {
        Stock stock = stockDAO.checkStock(id);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    /**
     * 扣除库存
     *
     * @param stock
     */
    private void updateSale(Stock stock) {
        // 在sql层面完成销量 sale+1 和版本号 version+1，并且根据商品id和版本号同时更新的商品。
        int updateRows = stockDAO.updateSale(stock);
        if (updateRows == 0) {
            throw new RuntimeException("抢购失败，请重试!!!");
        }
    }

    /**
     * 创建订单
     *
     * @param stock
     * @return
     */
    private Integer createOrder(Stock stock) {
        Order order = new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
        orderDAO.createOrder(order);
        return order.getId();
    }
}
