package club.lightingsummer.movie.gateway.controller;

import club.lightingsummer.movie.gateway.interceptor.HostHolder;
import club.lightingsummer.movie.gateway.vo.ResponseVO;
import club.lightingsummer.movie.order.api.api.OrderInfoAPI;
import club.lightingsummer.movie.order.api.vo.OrderVO;
import club.lightingsummer.movie.order.api.vo.Page;
import com.alibaba.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author     ：lightingSummer
 * @date       ：2019/7/30 0030
 * @description：
 */
@RestController
@RequestMapping(value = "/order/")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Reference(interfaceClass = OrderInfoAPI.class, check = false)
    private OrderInfoAPI orderInfoAPI;
    @Autowired
    private HostHolder hostHolder;

    /**
     * @author: lightingSummer
     * @date: 2019/8/4 0004
     * @description: 购票
     */
    @RequestMapping(value = "buyTickets", method = RequestMethod.POST)
    public ResponseVO buyTickets(Integer fieldId, String soldSeats, String seatsName) {
        try {
            // 参数合法性校验
            if (fieldId == null || soldSeats == null || seatsName == null) {
                return ResponseVO.serviceFail("参数不合法");
            }
            // 验证用户登录信息
            if(hostHolder.getUser() == null){
                return ResponseVO.serviceFail("用户未登录");
            }
            // 验证座位合法性
            if (!orderInfoAPI.isTrueSeats(fieldId + "", soldSeats)) {
                return ResponseVO.serviceFail("该座位不存在");
            }
            // 已经售出的票，有没有要请求的票
            if (!orderInfoAPI.isNotSoldSeats(fieldId + "", soldSeats)) {
                return ResponseVO.serviceFail("所选座位已经售出");
            }
            // 创建订单信息
            OrderVO orderVO = orderInfoAPI.saveOrderInfo(fieldId, soldSeats, seatsName, hostHolder.getUser());
            return ResponseVO.success(orderVO);
        } catch (Exception e) {
            logger.error("购票失败" + e.getMessage());
            return ResponseVO.serviceFail("购票失败，系统异常");
        }
    }

    /**
     * @author: lightingSummer
     * @date: 2019/8/4 0004
     * @description: 用户购票记录查询
     */
    @RequestMapping(value = "getOrderInfo", method = RequestMethod.POST)
    public ResponseVO getOrderInfo(
            @RequestParam(name = "nowPage", required = false, defaultValue = "1") Integer nowPage,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize
    ) {
        try {
            // 获取当前登录人的信息
            Integer userId = hostHolder.getUser();
            if (userId == null) {
                return ResponseVO.serviceFail("用户未登录");
            }
            Page<OrderVO> request = new Page<>(nowPage, pageSize);
            // 查当前人的订单信息
            Page<OrderVO> orderInfo = orderInfoAPI.getOrderByUserId(userId, request);
            return ResponseVO.success(orderInfo);
        } catch (Exception e) {
            logger.error("用户订单查询失败"+e.getMessage());
            return ResponseVO.serviceFail("用户订单查询失败");
        }
    }
}