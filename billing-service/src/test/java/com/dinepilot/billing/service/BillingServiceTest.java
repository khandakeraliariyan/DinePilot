package com.dinepilot.billing.service;

import com.dinepilot.billing.client.OrderClient;
import com.dinepilot.billing.entity.Invoice;
import com.dinepilot.billing.enums.InvoiceStatus;
import com.dinepilot.billing.repository.InvoiceRepository;
import com.dinepilot.billing.repository.PaymentRepository;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BillingServiceTest {
    private final OrderClient orderClient = mock(OrderClient.class);
    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final BillingService service = new BillingService(orderClient, invoiceRepository, paymentRepository);

    @Test
    void generateInvoiceCopiesTotalFromOrder() {
        when(invoiceRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        when(orderClient.getOrder("o1")).thenReturn(new OrderClient.OrderSnapshot("o1", "u1", "r1", "COMPLETED", BigDecimal.valueOf(120)));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = service.generateInvoice("o1", "u1", false);

        assertThat(invoice.getOrderId()).isEqualTo("o1");
        assertThat(invoice.getAmount()).isEqualByComparingTo("120");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void generateInvoiceRejectsNonOwner() {
        when(invoiceRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        when(orderClient.getOrder("o1")).thenReturn(new OrderClient.OrderSnapshot("o1", "u1", "r1", "COMPLETED", BigDecimal.TEN));
        assertThatThrownBy(() -> service.generateInvoice("o1", "u2", false)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void payInvoiceMarksInvoicePaid() {
        Invoice invoice = new Invoice();
        invoice.setOrderId("o1");
        invoice.setUserId("u1");
        invoice.setRestaurantId("r1");
        invoice.setAmount(BigDecimal.valueOf(50));
        invoice.setStatus(InvoiceStatus.PENDING);
        when(invoiceRepository.findById("i1")).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.payInvoice("i1", "u1", false);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(paymentRepository, atLeastOnce()).save(any());
    }

    @Test
    void getInvoiceRejectsMissingRecord() {
        when(invoiceRepository.findById("i1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getInvoice("i1", "u1", false)).isInstanceOf(ResourceNotFoundException.class);
    }
}
