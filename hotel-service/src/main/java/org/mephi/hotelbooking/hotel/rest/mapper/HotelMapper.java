package org.mephi.hotelbooking.hotel.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.mephi.hotelbooking.hotel.rest.dto.CreateHotelRequest;
import org.mephi.hotelbooking.hotel.rest.dto.HotelResponse;

@Mapper
public interface HotelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Hotel toEntity(CreateHotelRequest request);

    HotelResponse toResponse(Hotel hotel, Long roomsCount);
}
