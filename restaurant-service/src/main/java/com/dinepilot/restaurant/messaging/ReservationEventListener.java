package com.dinepilot.restaurant.messaging;

import com.dinepilot.common.event.ReservationCreatedEvent;
import com.dinepilot.common.event.ReservationStatusChangedEvent;
import com.dinepilot.restaurant.entity.RestaurantTable;
import com.dinepilot.restaurant.enums.TableStatus;
import com.dinepilot.restaurant.repository.RestaurantTableRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {
    private final RestaurantTableRepository tables;

    public ReservationEventListener(RestaurantTableRepository tables) { this.tables = tables; }

    @RabbitListener(queues = "reservation.created.queue")
    public void onReservationCreated(ReservationCreatedEvent event) {
        updateTableStatus(event.tableId(), TableStatus.RESERVED);
    }

    @RabbitListener(queues = "reservation.status.changed.queue")
    public void onReservationStatusChanged(ReservationStatusChangedEvent event) {
        if ("CANCELLED".equals(event.status()) || "COMPLETED".equals(event.status())) {
            updateTableStatus(event.tableId(), TableStatus.AVAILABLE);
        } else if ("CONFIRMED".equals(event.status())) {
            updateTableStatus(event.tableId(), TableStatus.RESERVED);
        }
    }

    private void updateTableStatus(String tableId, TableStatus status) {
        RestaurantTable table = tables.findById(tableId).orElse(null);
        if (table == null) return;
        table.setStatus(status);
        tables.save(table);
    }
}
