package com.macro.mall.portal.component;

import com.macro.mall.portal.service.OmsPortalOrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * 取消订单消息的处理者
 * Created by macro on 2018/9/14.
 */
@Component
public class CancelOrderReceiver {
    private static final Logger LOGGER =LoggerFactory.getLogger(CancelOrderReceiver.class);
    @Autowired
    private OmsPortalOrderService portalOrderService;

    @RabbitListener(queues = "mall.order.cancel")
    public void handle(Message message, Channel channel){

        // 获取消息的deliveryTag
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        // 获取订单编号
        String orderSn = new String(message.getBody());
        LOGGER.info("process orderSn:{}", orderSn);

        try {

            portalOrderService.cancelOrder(orderSn);

            // 手动ack，第一个参数是消息的deliveryTag，第二个参数是是否批量处理
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                LOGGER.error("确认订单" + orderSn + "异常", e);
            }
        } catch (Exception e) {
            LOGGER.error("取消订单" + orderSn + "异常", e);

            try {
                // 不确认消息，第一个参数是消息的deliveryTag，第二个参数是是否批量处理，第三个参数是是否重新入队列
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                LOGGER.error("重新投递订单" + orderSn + "异常", e);
            }
        }
    }
}
