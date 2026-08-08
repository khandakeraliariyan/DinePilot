package com.dinepilot.reservation.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.reservation.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter @Setter
@Document(collection = "reservations")
public class Reservation extends BaseEntity {
    @Indexed private String userId;
    @Indexed private String restaurantId;
    @Indexed private String tableId;
    private int partySize;
    private Instant reservedFor;
    @Indexed private ReservationStatus status;
    private String notes;
}
