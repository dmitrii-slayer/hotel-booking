package org.mephi.hotelbooking.booking.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.mephi.hotelbooking.booking.rest.dto.BookingResponse;
import org.mephi.hotelbooking.booking.rest.dto.UserResponse;

@Mapper
public interface BookingMapper {

    @Mapping(target = "username", source = "username")
    @Mapping(target = "roomNumber", source = "roomNumber")
    BookingResponse toResponse(Booking booking, String username, String roomNumber);

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toUserResponse(User user);
}
