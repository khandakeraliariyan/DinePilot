package com.dinepilot.reservation.repository;

import com.dinepilot.reservation.entity.Reservation;
import com.dinepilot.reservation.enums.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Reservation> findByRestaurantIdOrderByReservedForAsc(String restaurantId);
    List<Reservation> findByTableIdAndStatusIn(String tableId, List<ReservationStatus> statuses);
}
