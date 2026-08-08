package com.dinepilot.reservation.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.reservation.client.RestaurantServiceClient;
import com.dinepilot.reservation.dto.ReservationRequest;
import com.dinepilot.reservation.dto.ReservationResponse;
import com.dinepilot.reservation.entity.Reservation;
import com.dinepilot.reservation.enums.ReservationStatus;
import com.dinepilot.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final String RESTAURANT_ID = "restaurant-1";
    private static final String TABLE_ID = "table-1";
    private static final String RESERVATION_ID = "reservation-1";
    private static final Instant RESERVED_FOR = Instant.parse("2026-09-01T19:00:00Z");

    @Mock
    private ReservationRepository reservations;

    @Mock
    private RestaurantServiceClient restaurantClient;

    @Mock
    private RestaurantAccessGuard restaurantAccessGuard;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservations, restaurantClient, restaurantAccessGuard);
    }

    @Nested
    @DisplayName("booking a reservation")
    class Booking {

        @Test
        void createsAPendingReservationDerivedFromTheTable() {
            when(restaurantClient.getTable(TABLE_ID)).thenReturn(tableInfo(TABLE_ID, RESTAURANT_ID, 4));
            when(reservations.findByTableIdAndStatusIn(eq(TABLE_ID), anyList())).thenReturn(List.of());
            when(reservations.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation saved = invocation.getArgument(0);
                saved.setId(RESERVATION_ID);
                saved.setCreatedAt(Instant.parse("2026-08-08T10:15:30Z"));
                return saved;
            });

            ReservationResponse response = service.book(USER_ID, request(TABLE_ID, 2, RESERVED_FOR, "Birthday"));

            assertThat(response.id()).isEqualTo(RESERVATION_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
            assertThat(response.tableId()).isEqualTo(TABLE_ID);
            assertThat(response.partySize()).isEqualTo(2);
            assertThat(response.reservedFor()).isEqualTo(RESERVED_FOR);
            assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(response.notes()).isEqualTo("Birthday");
        }

        @Test
        void rejectsAPartySizeThatExceedsTableCapacity() {
            when(restaurantClient.getTable(TABLE_ID)).thenReturn(tableInfo(TABLE_ID, RESTAURANT_ID, 2));

            assertThatThrownBy(() -> service.book(USER_ID, request(TABLE_ID, 4, RESERVED_FOR, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Party size exceeds table capacity");

            verify(reservations, never()).findByTableIdAndStatusIn(any(), any());
            verify(reservations, never()).save(any());
        }

        @Test
        void rejectsAnOverlappingActiveReservationOnTheSameTable() {
            when(restaurantClient.getTable(TABLE_ID)).thenReturn(tableInfo(TABLE_ID, RESTAURANT_ID, 4));
            Reservation existing = reservation(RESERVATION_ID, OTHER_USER_ID, RESTAURANT_ID, TABLE_ID,
                    ReservationStatus.CONFIRMED, RESERVED_FOR.minus(30, ChronoUnit.MINUTES));
            when(reservations.findByTableIdAndStatusIn(eq(TABLE_ID), anyList())).thenReturn(List.of(existing));

            assertThatThrownBy(() -> service.book(USER_ID, request(TABLE_ID, 2, RESERVED_FOR, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Table is already reserved for the requested time");

            verify(reservations, never()).save(any());
        }

        @Test
        void allowsABackToBackBookingThatStartsWhenThePriorSlotEnds() {
            Instant priorStart = RESERVED_FOR.minus(90, ChronoUnit.MINUTES);
            when(restaurantClient.getTable(TABLE_ID)).thenReturn(tableInfo(TABLE_ID, RESTAURANT_ID, 4));
            Reservation existing = reservation(RESERVATION_ID, OTHER_USER_ID, RESTAURANT_ID, TABLE_ID,
                    ReservationStatus.CONFIRMED, priorStart);
            when(reservations.findByTableIdAndStatusIn(eq(TABLE_ID), anyList())).thenReturn(List.of(existing));
            when(reservations.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponse response = service.book(USER_ID, request(TABLE_ID, 2, RESERVED_FOR, null));

            assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        void onlyQueriesActiveReservationsForTheOverlapCheck() {
            when(restaurantClient.getTable(TABLE_ID)).thenReturn(tableInfo(TABLE_ID, RESTAURANT_ID, 4));
            when(reservations.findByTableIdAndStatusIn(any(), any())).thenReturn(List.of());
            when(reservations.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.book(USER_ID, request(TABLE_ID, 2, RESERVED_FOR, null));

            ArgumentCaptor<List<ReservationStatus>> captor = ArgumentCaptor.forClass(List.class);
            verify(reservations).findByTableIdAndStatusIn(eq(TABLE_ID), captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("customer reservation access")
    class CustomerAccess {

        @Test
        void returnsHistoryFromTheRepositoryAsIs() {
            Reservation newest = reservation("r-2", USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            Reservation older = reservation("r-1", USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.COMPLETED, RESERVED_FOR);
            when(reservations.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(newest, older));

            List<ReservationResponse> history = service.history(USER_ID);

            assertThat(history).extracting(ReservationResponse::id).containsExactly("r-2", "r-1");
        }

        @Test
        void returnsAnOwnedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            ReservationResponse response = service.getForCustomer(USER_ID, RESERVATION_ID);

            assertThat(response.id()).isEqualTo(RESERVATION_ID);
        }

        @Test
        void hidesAnotherCustomersReservation() {
            Reservation reservation = reservation(RESERVATION_ID, OTHER_USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.getForCustomer(USER_ID, RESERVATION_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This reservation belongs to another customer");
        }

        @Test
        void reportsAMissingReservation() {
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getForCustomer(USER_ID, RESERVATION_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Reservation not found");
        }

        @Test
        void allowsTheOwnerToCancelAPendingReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservations.save(reservation)).thenReturn(reservation);

            ReservationResponse response = service.cancelByCustomer(USER_ID, RESERVATION_ID);

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void allowsTheOwnerToCancelAConfirmedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.CONFIRMED, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservations.save(reservation)).thenReturn(reservation);

            ReservationResponse response = service.cancelByCustomer(USER_ID, RESERVATION_ID);

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void rejectsCancellationByAnotherCustomer() {
            Reservation reservation = reservation(RESERVATION_ID, OTHER_USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.cancelByCustomer(USER_ID, RESERVATION_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This reservation belongs to another customer");

            verify(reservations, never()).save(any());
        }

        @Test
        void rejectsCancellingAnAlreadyCompletedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.COMPLETED, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.cancelByCustomer(USER_ID, RESERVATION_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Reservation must be PENDING or CONFIRMED before it can become CANCELLED");

            verify(reservations, never()).save(any());
        }
    }

    @Nested
    @DisplayName("restaurant reservation management")
    class RestaurantManagement {

        private final Authentication authentication = mock(Authentication.class);

        @Test
        void listsReservationsAfterConfirmingRestaurantOwnership() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findByRestaurantIdOrderByReservedForAsc(RESTAURANT_ID)).thenReturn(List.of(reservation));

            List<ReservationResponse> response = service.forRestaurant(authentication, RESTAURANT_ID);

            assertThat(response).extracting(ReservationResponse::id).containsExactly(RESERVATION_ID);
            verify(restaurantAccessGuard).checkManagesRestaurant(authentication, RESTAURANT_ID);
        }

        @Test
        void propagatesAnOwnershipFailureWithoutQueryingReservations() {
            doThrow(new ForbiddenException("You do not manage this restaurant"))
                    .when(restaurantAccessGuard).checkManagesRestaurant(authentication, RESTAURANT_ID);

            assertThatThrownBy(() -> service.forRestaurant(authentication, RESTAURANT_ID))
                    .isInstanceOf(ForbiddenException.class);

            verify(reservations, never()).findByRestaurantIdOrderByReservedForAsc(any());
        }

        @Test
        void confirmsAPendingReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservations.save(reservation)).thenReturn(reservation);

            ReservationResponse response = service.confirm(authentication, RESERVATION_ID);

            assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(restaurantAccessGuard).checkManagesRestaurant(authentication, RESTAURANT_ID);
        }

        @Test
        void rejectsConfirmingAnAlreadyConfirmedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.CONFIRMED, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.confirm(authentication, RESERVATION_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Reservation must be PENDING before it can become CONFIRMED");
        }

        @Test
        void completesAConfirmedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.CONFIRMED, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservations.save(reservation)).thenReturn(reservation);

            ReservationResponse response = service.completeByRestaurant(authentication, RESERVATION_ID);

            assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        void rejectsCompletingAReservationThatWasNeverConfirmed() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.completeByRestaurant(authentication, RESERVATION_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Reservation must be CONFIRMED before it can become COMPLETED");
        }

        @Test
        void cancelsAPendingReservationOnBehalfOfTheRestaurant() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.PENDING, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservations.save(reservation)).thenReturn(reservation);

            ReservationResponse response = service.cancelByRestaurant(authentication, RESERVATION_ID);

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void rejectsCancellingACompletedReservation() {
            Reservation reservation = reservation(RESERVATION_ID, USER_ID, RESTAURANT_ID, TABLE_ID, ReservationStatus.COMPLETED, RESERVED_FOR);
            when(reservations.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.cancelByRestaurant(authentication, RESERVATION_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Reservation must be PENDING or CONFIRMED before it can become CANCELLED");

            verify(reservations, never()).save(any());
        }
    }

    private ReservationRequest request(String tableId, int partySize, Instant reservedFor, String notes) {
        return new ReservationRequest(tableId, partySize, reservedFor, notes);
    }

    private RestaurantServiceClient.TableInfo tableInfo(String id, String restaurantId, int capacity) {
        return new RestaurantServiceClient.TableInfo(id, restaurantId, "T1", capacity);
    }

    private Reservation reservation(
            String id,
            String userId,
            String restaurantId,
            String tableId,
            ReservationStatus status,
            Instant reservedFor
    ) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUserId(userId);
        reservation.setRestaurantId(restaurantId);
        reservation.setTableId(tableId);
        reservation.setPartySize(2);
        reservation.setReservedFor(reservedFor);
        reservation.setStatus(status);
        reservation.setCreatedAt(Instant.parse("2026-08-08T10:15:30Z"));
        reservation.setUpdatedAt(Instant.parse("2026-08-08T10:20:30Z"));
        return reservation;
    }
}
