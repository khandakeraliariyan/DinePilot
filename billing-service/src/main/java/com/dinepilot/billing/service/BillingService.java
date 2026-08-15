package com.dinepilot.billing.service;

import com.dinepilot.billing.client.OrderClient;
import com.dinepilot.billing.entity.Invoice;
import com.dinepilot.billing.entity.Payment;
import com.dinepilot.billing.enums.InvoiceStatus;
import com.dinepilot.billing.enums.PaymentStatus;
import com.dinepilot.billing.repository.InvoiceRepository;
import com.dinepilot.billing.repository.PaymentRepository;
import com.dinepilot.common.event.EventFactory;
import com.dinepilot.common.event.PaymentCompletedEvent;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class BillingService {
    private final OrderClient orderClient;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public BillingService(OrderClient orderClient, InvoiceRepository invoiceRepository, PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate) {
        this.orderClient = orderClient;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Invoice generateInvoice(String orderId, String requesterUserId, boolean elevated) {
        invoiceRepository.findByOrderId(orderId).ifPresent(existing -> { throw new ConflictException("Invoice already exists for this order"); });
        OrderClient.OrderSnapshot order = orderClient.getOrder(orderId);
        if (!elevated && !order.userId().equals(requesterUserId)) throw new ForbiddenException("You do not own this order");
        if (!"COMPLETED".equalsIgnoreCase(order.status())) throw new ConflictException("Invoice can only be generated from a completed order");
        Invoice invoice = new Invoice();
        invoice.setOrderId(order.id());
        invoice.setUserId(order.userId());
        invoice.setRestaurantId(order.restaurantId());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setAmount(order.total());
        invoice.setReceiptNumber(null);
        return invoiceRepository.save(invoice);
    }

    public Invoice generateInvoiceFromEvent(String orderId) {
        invoiceRepository.findByOrderId(orderId).ifPresent(existing -> { throw new ConflictException("Invoice already exists for this order"); });
        OrderClient.OrderSnapshot order = orderClient.getOrder(orderId);
        if (!"COMPLETED".equalsIgnoreCase(order.status())) throw new ConflictException("Invoice can only be generated from a completed order");
        Invoice invoice = new Invoice();
        invoice.setOrderId(order.id());
        invoice.setUserId(order.userId());
        invoice.setRestaurantId(order.restaurantId());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setAmount(order.total());
        return invoiceRepository.save(invoice);
    }

    public Payment payInvoice(String invoiceId, String requesterUserId, boolean elevated) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!elevated && !invoice.getUserId().equals(requesterUserId)) throw new ForbiddenException("You do not own this invoice");
        if (invoice.getStatus() == InvoiceStatus.PAID) throw new ConflictException("Invoice is already paid");
        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(invoice.getAmount());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProcessedAt(Instant.now());
        payment.setProviderRef("SIM-" + Instant.now().toEpochMilli());
        payment = paymentRepository.save(payment);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(Instant.now());
        paymentRepository.save(payment);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setReceiptNumber("R-" + payment.getProviderRef());
        invoiceRepository.save(invoice);
        rabbitTemplate.convertAndSend("dinepilot.events", "payment.completed",
                new PaymentCompletedEvent(EventFactory.eventId(), EventFactory.now(), payment.getId(), invoiceId,
                        invoice.getOrderId(), invoice.getUserId(), invoice.getRestaurantId(), payment.getAmount()));
        return payment;
    }

    public List<Invoice> listMyInvoices(String userId) { return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId); }
    public Invoice getInvoice(String invoiceId, String requesterUserId, boolean elevated) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (!elevated && !invoice.getUserId().equals(requesterUserId)) throw new ForbiddenException("You do not own this invoice");
        return invoice;
    }
}
