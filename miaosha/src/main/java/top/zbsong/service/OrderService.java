package top.zbsong.service;

public interface OrderService {
    /**
     * 处理秒杀的下单方法，并返回订单的id
     * @param id
     * @return
     */
    int kill(Integer id);

    /**
     * 根据商品id和用户id生成md5值
     * @param id
     * @param userid
     * @return
     */
    String getMd5(Integer id, Integer userid);

    /**
     * 根据商品id，用户id，和md5处理秒杀的下单方法（抢购接口隐藏）
     * @param id
     * @param userid
     * @param md5
     * @return
     */
    int kill(Integer id, Integer userid, String md5);
}
