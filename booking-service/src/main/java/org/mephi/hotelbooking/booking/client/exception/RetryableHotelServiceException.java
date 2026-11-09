package org.mephi.hotelbooking.booking.client.exception;

public class RetryableHotelServiceException extends HotelServiceException {

    public RetryableHotelServiceException(String message) {
        super(message);
    }

    public RetryableHotelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
