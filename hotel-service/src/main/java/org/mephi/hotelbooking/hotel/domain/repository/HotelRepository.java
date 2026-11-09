package org.mephi.hotelbooking.hotel.domain.repository;

import org.mephi.hotelbooking.hotel.domain.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    List<Hotel> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
