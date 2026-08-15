package com.dinepilot.billing.messaging;

import com.dinepilot.billing.service.BillingService;
import com.dinepilot.common.event.OrderStatusChangedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCompletedListener {
    private final BillingService billingService;

    public OrderCompletedListener(BillingService billingService) { this.billingService = billingService; }

    @RabbitListener(queues = "order.status.changed.queue")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if ("COMPLETED".equals(event.status())) {
            billingService.generateInvoiceFromEvent(event.orderId());
        }
    }
}
