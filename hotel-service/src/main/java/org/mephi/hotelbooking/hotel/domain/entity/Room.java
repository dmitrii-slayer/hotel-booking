package org.mephi.hotelbooking.hotel.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.mephi.hotelbooking.hotel.domain.enums.RoomType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "rooms_hotel_id_number_key",
                columnNames = {"hotel_id", "number"}),
        indexes = {
                @Index(name = "rooms_hotel_id_idx", columnList = "hotel_id"),
                @Index(name = "rooms_times_booked_idx", columnList = "times_booked"),
        })
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "times_booked", nullable = false)
    @Builder.Default
    private Long timesBooked = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public void incrementTimesBooked() {
        this.timesBooked = this.timesBooked + 1;
    }
}
