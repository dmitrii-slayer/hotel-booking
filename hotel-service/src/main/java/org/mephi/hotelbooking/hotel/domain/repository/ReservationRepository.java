package org.mephi.hotelbooking.hotel.domain.repository;

import org.mephi.hotelbooking.hotel.domain.entity.Reservation;
import org.mephi.hotelbooking.hotel.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByRoomIdAndRequestId(UUID roomId, String requestId);

    List<Reservation> findByBookingId(String bookingId);

    List<Reservation> findByRoomId(UUID roomId);

    default List<Reservation> findConflicting(UUID roomId, LocalDate startDate, LocalDate endDate) {
        return findConflicting(roomId, startDate, endDate, ReservationStatus.ACTIVE);
    }

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.roomId = :roomId
            AND r.status IN :statuses
            AND r.startDate < :endDate
            AND :startDate < r.endDate
            """)
    List<Reservation> findConflicting(@Param("roomId") UUID roomId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("statuses") List<ReservationStatus> statuses);
}
