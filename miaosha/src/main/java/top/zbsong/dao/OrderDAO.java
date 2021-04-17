package top.zbsong.dao;

import top.zbsong.entity.Order;
import top.zbsong.entity.Stock;

public interface OrderDAO {
    /**
     * 创建订单方法
     * @param order
     */
    void createOrder(Order order);
}
