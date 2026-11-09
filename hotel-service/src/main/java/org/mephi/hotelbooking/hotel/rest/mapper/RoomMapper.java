package org.mephi.hotelbooking.hotel.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mephi.hotelbooking.hotel.domain.entity.Room;
import org.mephi.hotelbooking.hotel.rest.dto.CreateRoomRequest;
import org.mephi.hotelbooking.hotel.rest.dto.RoomResponse;
import org.mephi.hotelbooking.hotel.rest.dto.UpdateRoomRequest;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelId", source = "hotelId")
    @Mapping(target = "available", source = "request.available", defaultValue = "true")
    @Mapping(target = "timesBooked", expression = "java(0L)")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Room toEntity(CreateRoomRequest request, java.util.UUID hotelId);

    RoomResponse toResponse(Room room, String hotelName);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotelId", ignore = true)
    @Mapping(target = "timesBooked", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateRoomRequest request, @MappingTarget Room room);
}
