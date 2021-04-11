package top.zbsong.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.zbsong.service.OrderService;

@Controller
@RequestMapping("stock")
public class StockController {

    @Autowired
    private OrderService orderService;

    /**
     * 根据秒杀商品id去调用秒杀业务
     *
     * @param id
     * @return
     */
    @GetMapping("kill")
    public String kill(Integer id) {
        System.out.println("秒杀商品的id=" + id);
        int orderId = orderService.kill(id);
        return "秒杀成功，订单id为" + String.valueOf(orderId);
    }
}
