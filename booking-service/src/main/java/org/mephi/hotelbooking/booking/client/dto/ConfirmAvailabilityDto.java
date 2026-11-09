package org.mephi.hotelbooking.booking.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityDto {

    private String requestId;
    private String bookingId;
    private LocalDate startDate;
    private LocalDate endDate;
}
