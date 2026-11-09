package org.mephi.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.booking.client.HotelServiceGateway;
import org.mephi.hotelbooking.booking.client.dto.ReleaseDto;
import org.mephi.hotelbooking.booking.client.dto.RoomDto;
import org.mephi.hotelbooking.booking.domain.entity.Booking;
import org.mephi.hotelbooking.booking.domain.entity.User;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.domain.repository.BookingRepository;
import org.mephi.hotelbooking.booking.domain.repository.UserRepository;
import org.mephi.hotelbooking.booking.exception.AccessForbiddenException;
import org.mephi.hotelbooking.booking.exception.BookingNotCancellableException;
import org.mephi.hotelbooking.booking.exception.EntityNotFoundException;
import org.mephi.hotelbooking.booking.rest.dto.BookingResponse;
import org.mephi.hotelbooking.booking.rest.dto.CreateBookingRequest;
import org.mephi.hotelbooking.booking.rest.dto.PageResponse;
import org.mephi.hotelbooking.booking.rest.mapper.BookingMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingSagaService sagaService;
    private final BookingStateService stateService;
    private final HotelServiceGateway hotelGateway;
    private final BookingMapper bookingMapper;

    /**
     * Создание бронирования. Идемпотентно по requestId:
     * повторная доставка возвращает существующую бронь без создания дубликата.
     */
    public BookingResponse createBooking(UUID userId, String username,
                                         CreateBookingRequest request, String traceId, String bearerToken) {
        String requestId = request.ensureRequestId();

        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("[saga][{}] Повторный запрос на создание проигнорирован (идемпотентность): requestId={}",
                    existing.get().getId(), requestId);

            String existingRoomNumber = resolveRoomNumber(existing.get().getRoomId(), traceId, bearerToken);
            return bookingMapper.toResponse(existing.get(), username, existingRoomNumber);
        }

        validateDates(request);

        UUID roomId = resolveRoomId(request, traceId, bearerToken);
        UUID hotelId = resolveHotelId(roomId, traceId, bearerToken);

        Booking pending = createPendingOrReturnExisting(userId, roomId, hotelId, request, traceId);
        Booking result = sagaService.confirmOrCompensate(pending, traceId, bearerToken);

        if (result.isCancelled()) {
            log.warn("[saga][{}] Бронирование отменено в результате компенсации", result.getId());
        }

        String roomNumber = resolveRoomNumber(result.getRoomId(), traceId, bearerToken);
        return bookingMapper.toResponse(result, username, roomNumber);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(UUID userId, String username,
                                                         BookingStatus status, Pageable pageable) {
        Page<Booking> page = status == null
                ? bookingRepository.findByUserId(userId, pageable)
                : bookingRepository.findByUserIdAndStatus(userId, status, pageable);

        return toPage(page, username);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID userId, boolean admin,
                                      String traceId, String bearerToken) {
        Booking booking = findBooking(bookingId);
        checkOwnership(booking, userId, admin);

        return bookingMapper.toResponse(booking,
                resolveUsername(booking.getUserId()),
                resolveRoomNumber(booking.getRoomId(), traceId, bearerToken));
    }

    public BookingResponse cancelBooking(UUID bookingId, UUID userId, boolean admin,
                                         String traceId, String bearerToken) {
        Booking booking = findBooking(bookingId);
        checkOwnership(booking, userId, admin);

        if (booking.isCancelled()) {
            log.info("[saga][{}] Бронирование уже отменено (идемпотентность)", bookingId);
            return bookingMapper.toResponse(booking,
                    resolveUsername(booking.getUserId()),
                    resolveRoomNumber(booking.getRoomId(), traceId, bearerToken));
        }

        if (booking.getStartDate().isBefore(LocalDate.now())) {
            throw new BookingNotCancellableException(
                    "Нельзя отменить бронирование, срок заезда которого уже наступил: "
                            + booking.getStartDate());
        }

        Booking cancelled = stateService.markCancelled(bookingId, "Отменено пользователем");
        releaseSlot(cancelled, traceId, bearerToken);

        log.info("[saga][{}] Бронирование отменено пользователем", bookingId);

        return bookingMapper.toResponse(cancelled,
                resolveUsername(cancelled.getUserId()),
                resolveRoomNumber(cancelled.getRoomId(), traceId, bearerToken));
    }

    private String resolveUsername(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    /**
     * Создаёт PENDING-бронь либо возвращает уже существующую с тем же requestId.
     */
    private Booking createPendingOrReturnExisting(UUID userId, UUID roomId, UUID hotelId,
                                                  CreateBookingRequest request, String traceId) {
        try {
            return sagaService.createPending(userId, roomId, hotelId, request, traceId);
        } catch (DataIntegrityViolationException ex) {
            log.info("[saga] Параллельный запрос с тем же requestId={}, возвращаю существующую бронь",
                    request.getRequestId());

            return bookingRepository.findByRequestId(request.getRequestId())
                    .orElseThrow(() -> ex);
        }
    }

    private void releaseSlot(Booking booking, String traceId, String bearerToken) {
        ReleaseDto request = ReleaseDto.builder()
                .requestId(booking.getRequestId())
                .bookingId(booking.getId().toString())
                .build();

        try {
            hotelGateway.release(booking.getRoomId(), request, traceId, bearerToken);
        } catch (Exception ex) {
            log.error("[saga][{}] Не удалось снять блокировку при отмене: {}",
                    booking.getId(), ex.getMessage());
        }
    }

    private UUID resolveRoomId(CreateBookingRequest request, String traceId, String bearerToken) {
        if (request.isAutoSelectEnabled()) {
            RoomDto selected = sagaService.selectRoom(request.getStartDate(), request.getEndDate(),
                    traceId, bearerToken);
            log.info("[saga] Автоподбор номера: {} (timesBooked={})",
                    selected.getNumber(), selected.getTimesBooked());
            return selected.getId();
        }

        return request.getRoomId();
    }

    private UUID resolveHotelId(UUID roomId, String traceId, String bearerToken) {
        try {
            RoomDto room = hotelGateway.getRoom(roomId, traceId, bearerToken);
            return room != null ? room.getHotelId() : null;
        } catch (Exception ex) {
            log.warn("Не удалось получить отель для номера {}: {}", roomId, ex.getMessage());
            return null;
        }
    }

    private String resolveRoomNumber(UUID roomId, String traceId, String bearerToken) {
        try {
            RoomDto room = hotelGateway.getRoom(roomId, traceId, bearerToken);
            return room != null ? room.getNumber() : null;
        } catch (Exception ex) {
            log.warn("Не удалось получить номер комнаты {}: {}", roomId, ex.getMessage());
            return null;
        }
    }

    private void validateDates(CreateBookingRequest request) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException(
                    "Дата выезда должна быть позже даты заезда: "
                            + request.getStartDate() + " - " + request.getEndDate());
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Дата заезда не может быть в прошлом: " + request.getStartDate());
        }
    }

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование не найдено: " + bookingId));
    }

    private void checkOwnership(Booking booking, UUID userId, boolean admin) {
        if (!admin && !booking.belongsTo(userId)) {
            throw new AccessForbiddenException("Бронирование принадлежит другому пользователю");
        }
    }

    private PageResponse<BookingResponse> toPage(Page<Booking> page, String username) {
        return PageResponse.<BookingResponse>builder()
                .content(page.getContent().stream()
                        .map(booking -> bookingMapper.toResponse(booking, username, null))
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
