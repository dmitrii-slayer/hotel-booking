package org.mephi.hotelbooking.booking.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {

    private UUID reservationId;
    private UUID roomId;
    private String requestId;
    private String bookingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean replayed;
    private Instant createdAt;
}
