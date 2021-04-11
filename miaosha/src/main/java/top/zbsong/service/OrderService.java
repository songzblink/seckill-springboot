package top.zbsong.service;

public interface OrderService {
    /**
     * 处理秒杀的下单方法，并返回订单的id
     * @param id
     * @return
     */
    int kill(Integer id);
}
