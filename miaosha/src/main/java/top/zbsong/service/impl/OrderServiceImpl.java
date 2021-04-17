package top.zbsong.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zbsong.dao.OrderDAO;
import top.zbsong.dao.StockDAO;
import top.zbsong.entity.Order;
import top.zbsong.entity.Stock;
import top.zbsong.service.OrderService;

import java.util.Date;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    private StockDAO stockDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Override
    public int kill(Integer id) {
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
