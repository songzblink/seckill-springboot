package top.zbsong.dao;

import top.zbsong.entity.Stock;

public interface StockDAO {
    /**
     * 根据商品id查询库存信息的方法
     * @param id
     * @return
     */
    Stock chechStock(Integer id);
}
