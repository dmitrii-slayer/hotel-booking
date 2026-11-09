package org.mephi.hotelbooking.hotel.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mephi.hotelbooking.hotel.rest.dto.AvailabilityResponse;
import org.mephi.hotelbooking.hotel.rest.dto.ConfirmAvailabilityRequest;
import org.mephi.hotelbooking.hotel.rest.dto.CreateRoomRequest;
import org.mephi.hotelbooking.hotel.rest.dto.ErrorDto;
import org.mephi.hotelbooking.hotel.rest.dto.ReleaseRequest;
import org.mephi.hotelbooking.hotel.rest.dto.RoomLoadResponse;
import org.mephi.hotelbooking.hotel.rest.dto.RoomResponse;
import org.mephi.hotelbooking.hotel.rest.dto.UpdateRoomRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/rooms")
@Tag(name = "Номера", description = "Просмотр, администрирование и статистика номеров")
@SecurityRequirement(name = "bearerAuth")
public interface RoomApi {

    @PostMapping
    @Operation(
            operationId = "createRoom",
            summary = "Добавить номер в отель",
            description = "Создаёт номер. Доступно только администратору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Номер создан",
                            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Отель не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request);

    @GetMapping
    @Operation(
            operationId = "getAvailableRooms",
            summary = "Получить список свободных номеров",
            description = "Все операционно доступные номера без специальной сортировки. "
                    + "Если переданы даты - исключаются занятые на этот период.",
            responses = @ApiResponse(responseCode = "200", description = "Список номеров получен",
                    content = @Content(schema = @Schema(implementation = RoomResponse[].class))))
    ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @Parameter(description = "Дата заезда (включительно)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Дата выезда (не включается)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate);

    @GetMapping("/recommend")
    @Operation(
            operationId = "getRecommendedRooms",
            summary = "Получить рекомендованные номера",
            description = "Свободные номера, отсортированные по возрастанию times_booked, при равенстве - по идентификатору.",
            responses = @ApiResponse(responseCode = "200", description = "Список номеров получен",
                    content = @Content(schema = @Schema(implementation = RoomResponse[].class))))
    ResponseEntity<List<RoomResponse>> getRecommendedRooms(
            @Parameter(description = "Дата заезда (включительно)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Дата выезда (не включается)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate);

    @GetMapping("/statistics")
    @Operation(
            operationId = "getRoomStatistics",
            summary = "Статистика загруженности номеров",
            description = "Загруженность на горизонте: количество слотов, ночей и процент занятости. Доступно администратору.",
            responses = @ApiResponse(responseCode = "200", description = "Статистика получена",
                    content = @Content(schema = @Schema(implementation = RoomLoadResponse[].class))))
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<List<RoomLoadResponse>> getRoomStatistics(
            @Parameter(description = "Начало горизонта (по умолчанию - сегодня)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate horizonStart,
            @Parameter(description = "Конец горизонта (по умолчанию - +1 месяц)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate horizonEnd);

    @GetMapping("/hotel/{hotelId}")
    @Operation(
            operationId = "getRoomsByHotel",
            summary = "Получить номера отеля",
            responses = @ApiResponse(responseCode = "200", description = "Список номеров получен",
                    content = @Content(schema = @Schema(implementation = RoomResponse[].class))))
    ResponseEntity<List<RoomResponse>> getRoomsByHotel(
            @Parameter(description = "Идентификатор отеля") @PathVariable UUID hotelId);

    @GetMapping("/{roomId}")
    @Operation(
            operationId = "getRoomById",
            summary = "Получить номер по идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Номер найден",
                            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Номер не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<RoomResponse> getRoomById(
            @Parameter(description = "Идентификатор номера") @PathVariable UUID roomId);

    @PatchMapping("/{roomId}")
    @Operation(
            operationId = "updateRoom",
            summary = "Частично обновить номер",
            description = "Обновляет переданные поля; null-поля игнорируются. Доступно администратору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Номер обновлён",
                            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Номер не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<RoomResponse> updateRoom(
            @Parameter(description = "Идентификатор номера") @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomRequest request);

    @DeleteMapping("/{roomId}")
    @Operation(
            operationId = "deleteRoom",
            summary = "Удалить номер",
            description = "Доступно администратору.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Номер удалён"),
                    @ApiResponse(responseCode = "404", description = "Номер не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deleteRoom(
            @Parameter(description = "Идентификатор номера") @PathVariable UUID roomId);

    @PostMapping("/{roomId}/confirm-availability")
    @Operation(
            operationId = "confirmAvailability",
            summary = "Подтвердить доступность номера",
            description = "Устанавливает временную блокировку слота на запрошенные даты. "
                    + "Внутренний эндпойнт Booking Service; идемпотентен по requestId. "
                    + "При конфликте возвращает 409.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Слот подтверждён",
                            content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Номер занят на запрошенные даты",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<AvailabilityResponse> confirmAvailability(
            @Parameter(description = "Идентификатор номера") @PathVariable UUID roomId,
            @Valid @RequestBody ConfirmAvailabilityRequest request);

    @PostMapping("/{roomId}/release")
    @Operation(
            operationId = "releaseAvailability",
            summary = "Снять временную блокировку слота",
            description = "Компенсирующее действие для саги. "
                    + "Идемпотентен: повторный вызов не меняет состояние.",
            responses = @ApiResponse(responseCode = "200", description = "Блокировка снята",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))))
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<AvailabilityResponse> releaseAvailability(
            @Parameter(description = "Идентификатор номера") @PathVariable UUID roomId,
            @Valid @RequestBody ReleaseRequest request);

    @PostMapping("/booking/{bookingId}/confirm")
    @Operation(
            operationId = "confirmBookingSlots",
            summary = "Зафиксировать слоты бронирования",
            description = "Переводит слоты бронирования из HOLD в CONFIRMED после успешного шага саги.",
            responses = @ApiResponse(responseCode = "200", description = "Слоты зафиксированы"))
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    ResponseEntity<Void> confirmBookingSlots(
            @Parameter(description = "Идентификатор бронирования") @PathVariable String bookingId);
}
