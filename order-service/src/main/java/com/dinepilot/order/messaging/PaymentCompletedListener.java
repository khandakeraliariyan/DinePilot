package com.dinepilot.order.messaging;

import com.dinepilot.common.event.PaymentCompletedEvent;
import com.dinepilot.order.service.OrderManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedListener {
    private final OrderManagementService orders;

    public PaymentCompletedListener(OrderManagementService orders) { this.orders = orders; }

    @RabbitListener(queues = "payment.completed.queue")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orders.markPaid(event.orderId());
    }
}
