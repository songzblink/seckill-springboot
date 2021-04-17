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
