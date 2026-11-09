package org.mephi.hotelbooking.hotel.domain.enums;

import java.util.List;

public enum ReservationStatus {

    HOLD,
    CONFIRMED,
    RELEASED;

    /**
     * Статусы, при которых слот занимает даты. RELEASED в них не входит.
     */
    public static final List<ReservationStatus> ACTIVE = List.of(HOLD, CONFIRMED);
}
