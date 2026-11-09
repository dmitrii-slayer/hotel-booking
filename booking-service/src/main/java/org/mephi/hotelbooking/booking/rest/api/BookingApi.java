package org.mephi.hotelbooking.booking.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mephi.hotelbooking.booking.domain.enums.BookingStatus;
import org.mephi.hotelbooking.booking.rest.dto.BookingResponse;
import org.mephi.hotelbooking.booking.rest.dto.CreateBookingRequest;
import org.mephi.hotelbooking.booking.rest.dto.ErrorDto;
import org.mephi.hotelbooking.booking.rest.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Бронирования", description = "Создание, просмотр и отмена бронирований")
@SecurityRequirement(name = "bearerAuth")
public interface BookingApi {

    @PostMapping("/booking")
    @Operation(
            operationId = "createBooking",
            summary = "Создать бронирование",
            description = "Создаёт бронирование в статусе PENDING, подтверждает доступность номера "
                    + "в Hotel Service и переводит в CONFIRMED. При сбое выполняется компенсация "
                    + "(статус CANCELLED, блокировка снимается). Идемпотентно по requestId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Бронирование создано",
                            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
                    @ApiResponse(responseCode = "401", description = "Не авторизован",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request);

    @GetMapping("/bookings")
    @Operation(
            operationId = "getUserBookings",
            summary = "История бронирований пользователя",
            description = "Возвращает только бронирования текущего пользователя с пагинацией "
                    + "и необязательным фильтром по статусу.",
            responses = @ApiResponse(responseCode = "200", description = "Список бронирований получен",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))))
    ResponseEntity<PageResponse<BookingResponse>> getUserBookings(
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable);

    @GetMapping("/booking/{bookingId}")
    @Operation(
            operationId = "getBookingById",
            summary = "Получить бронирование по идентификатору",
            description = "Пользователь видит только свои бронирования; администратор - любые.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Бронирование найдено",
                            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Бронирование принадлежит другому",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
                    @ApiResponse(responseCode = "404", description = "Бронирование не найдено",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID bookingId);

    @DeleteMapping("/booking/{bookingId}")
    @Operation(
            operationId = "cancelBooking",
            summary = "Отменить бронирование",
            description = "Переводит бронирование в CANCELLED и снимает блокировку слота в Hotel Service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Бронирование отменено",
                            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Отмена невозможна",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID bookingId);
}
