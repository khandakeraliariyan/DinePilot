package com.dinepilot.reservation.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.common.event.EventFactory;
import com.dinepilot.common.event.ReservationCreatedEvent;
import com.dinepilot.reservation.client.RestaurantServiceClient;
import com.dinepilot.reservation.dto.ReservationRequest;
import com.dinepilot.reservation.dto.ReservationResponse;
import com.dinepilot.reservation.entity.Reservation;
import com.dinepilot.reservation.enums.ReservationStatus;
import com.dinepilot.reservation.repository.ReservationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReservationService {
    static final Duration SLOT_DURATION = Duration.ofMinutes(90);
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservations;
    private final RestaurantServiceClient restaurantClient;
    private final RestaurantAccessGuard restaurantAccessGuard;
    private final RabbitTemplate rabbitTemplate;

    public ReservationService(ReservationRepository reservations, RestaurantServiceClient restaurantClient,
                               RestaurantAccessGuard restaurantAccessGuard, RabbitTemplate rabbitTemplate) {
        this.reservations = reservations;
        this.restaurantClient = restaurantClient;
        this.restaurantAccessGuard = restaurantAccessGuard;
        this.rabbitTemplate = rabbitTemplate;
    }

    public ReservationResponse book(String userId, ReservationRequest request) {
        RestaurantServiceClient.TableInfo table = restaurantClient.getTable(request.tableId());
        if (request.partySize() > table.capacity()) {
            throw new ConflictException("Party size exceeds table capacity");
        }
        assertNoOverlap(table.id(), request.reservedFor());

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setRestaurantId(table.restaurantId());
        reservation.setTableId(table.id());
        reservation.setPartySize(request.partySize());
        reservation.setReservedFor(request.reservedFor());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setNotes(request.notes());
        Reservation saved = reservations.save(reservation);
        publishCreated(saved);
        return toResponse(saved);
    }

    public List<ReservationResponse> history(String userId) {
        return reservations.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    public ReservationResponse getForCustomer(String userId, String id) {
        Reservation reservation = find(id);
        if (!reservation.getUserId().equals(userId)) throw new ForbiddenException("This reservation belongs to another customer");
        return toResponse(reservation);
    }

    public ReservationResponse cancelByCustomer(String userId, String id) {
        Reservation reservation = find(id);
        if (!reservation.getUserId().equals(userId)) throw new ForbiddenException("This reservation belongs to another customer");
        cancel(reservation);
        Reservation saved = reservations.save(reservation);
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    public List<ReservationResponse> forRestaurant(Authentication authentication, String restaurantId) {
        restaurantAccessGuard.checkManagesRestaurant(authentication, restaurantId);
        return reservations.findByRestaurantIdOrderByReservedForAsc(restaurantId).stream().map(this::toResponse).toList();
    }

    public ReservationResponse confirm(Authentication authentication, String id) {
        Reservation reservation = find(id);
        restaurantAccessGuard.checkManagesRestaurant(authentication, reservation.getRestaurantId());
        transition(reservation, ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        Reservation saved = reservations.save(reservation);
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    public ReservationResponse completeByRestaurant(Authentication authentication, String id) {
        Reservation reservation = find(id);
        restaurantAccessGuard.checkManagesRestaurant(authentication, reservation.getRestaurantId());
        transition(reservation, ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);
        Reservation saved = reservations.save(reservation);
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    public ReservationResponse cancelByRestaurant(Authentication authentication, String id) {
        Reservation reservation = find(id);
        restaurantAccessGuard.checkManagesRestaurant(authentication, reservation.getRestaurantId());
        cancel(reservation);
        return toResponse(reservations.save(reservation));
    }

    private void cancel(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("Reservation must be PENDING or CONFIRMED before it can become CANCELLED");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    private void transition(Reservation reservation, ReservationStatus required, ReservationStatus target) {
        if (reservation.getStatus() != required) {
            throw new ConflictException("Reservation must be " + required + " before it can become " + target);
        }
        reservation.setStatus(target);
    }

    private void assertNoOverlap(String tableId, Instant reservedFor) {
        Instant newStart = reservedFor;
        Instant newEnd = reservedFor.plus(SLOT_DURATION);
        boolean overlaps = reservations.findByTableIdAndStatusIn(tableId, ACTIVE_STATUSES).stream()
                .anyMatch(r -> {
                    Instant existingStart = r.getReservedFor();
                    Instant existingEnd = existingStart.plus(SLOT_DURATION);
                    return newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd);
                });
        if (overlaps) throw new ConflictException("Table is already reserved for the requested time");
    }

    private Reservation find(String id) {
        return reservations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getUserId(), reservation.getRestaurantId(),
                reservation.getTableId(), reservation.getPartySize(), reservation.getReservedFor(),
                reservation.getStatus(), reservation.getNotes(), reservation.getCreatedAt(), reservation.getUpdatedAt());
    }

    private void publishCreated(Reservation reservation) {
        rabbitTemplate.convertAndSend("dinepilot.events", "reservation.created",
                new ReservationCreatedEvent(EventFactory.eventId(), EventFactory.now(), reservation.getId(),
                        reservation.getUserId(), reservation.getRestaurantId(), reservation.getTableId(),
                        reservation.getReservedFor()));
    }

    private void publishStatusChanged(Reservation reservation) {
        rabbitTemplate.convertAndSend("dinepilot.events", "reservation.status.changed",
                new com.dinepilot.common.event.ReservationStatusChangedEvent(
                        EventFactory.eventId(), EventFactory.now(), reservation.getId(),
                        reservation.getRestaurantId(), reservation.getTableId(), reservation.getStatus().name()));
    }
}
