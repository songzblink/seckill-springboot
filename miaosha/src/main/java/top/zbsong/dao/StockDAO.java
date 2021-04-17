package top.zbsong.dao;

import top.zbsong.entity.Stock;

public interface StockDAO {
    /**
     * 根据商品id查询库存信息的方法
     * @param id
     * @return
     */
    Stock chechStock(Integer id);

    /**
     * 根据商品id扣除库存
     * @param stock
     */
    void updateSale(Stock stock);
}
