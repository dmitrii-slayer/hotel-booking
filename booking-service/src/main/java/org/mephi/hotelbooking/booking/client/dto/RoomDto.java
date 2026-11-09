package org.mephi.hotelbooking.booking.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {

    private UUID id;
    private UUID hotelId;
    private String hotelName;
    private String number;
    private String roomType;
    private Integer capacity;
    private Double pricePerNight;
    private Boolean available;
    private Long timesBooked;
}
