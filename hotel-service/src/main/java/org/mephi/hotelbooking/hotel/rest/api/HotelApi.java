package org.mephi.hotelbooking.hotel.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mephi.hotelbooking.hotel.rest.dto.CreateHotelRequest;
import org.mephi.hotelbooking.hotel.rest.dto.ErrorDto;
import org.mephi.hotelbooking.hotel.rest.dto.HotelResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/hotels")
@Tag(name = "Отели", description = "Просмотр и администрирование отелей")
@SecurityRequirement(name = "bearerAuth")
public interface HotelApi {

    @PostMapping
    @Operation(
            operationId = "createHotel",
            summary = "Добавить отель",
            description = "Создаёт новый отель. Доступно только администратору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Отель успешно создан",
                            content = @Content(schema = @Schema(implementation = HotelResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
                    @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
                    @ApiResponse(responseCode = "403", description = "Нет прав",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<HotelResponse> createHotel(@Valid @RequestBody CreateHotelRequest request);

    @GetMapping
    @Operation(
            operationId = "getHotels",
            summary = "Получить список отелей",
            description = "Возвращает список отелей с необязательным фильтром по названию.",
            responses = @ApiResponse(responseCode = "200", description = "Список отелей получен",
                    content = @Content(schema = @Schema(implementation = HotelResponse[].class))))
    ResponseEntity<List<HotelResponse>> getHotels(
            @Parameter(description = "Фильтр по названию (регистронезависимый)")
            @RequestParam(required = false) String name);

    @GetMapping("/{hotelId}")
    @Operation(
            operationId = "getHotelById",
            summary = "Получить отель по идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Отель найден",
                            content = @Content(schema = @Schema(implementation = HotelResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Отель не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<HotelResponse> getHotelById(
            @Parameter(description = "Идентификатор отеля") @PathVariable UUID hotelId);

    @PutMapping("/{hotelId}")
    @Operation(
            operationId = "updateHotel",
            summary = "Обновить отель",
            description = "Полностью обновляет данные отеля. Доступно только администратору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Отель обновлён",
                            content = @Content(schema = @Schema(implementation = HotelResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Отель не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<HotelResponse> updateHotel(
            @Parameter(description = "Идентификатор отеля") @PathVariable UUID hotelId,
            @Valid @RequestBody CreateHotelRequest request);

    @DeleteMapping("/{hotelId}")
    @Operation(
            operationId = "deleteHotel",
            summary = "Удалить отель",
            description = "Удаляет отель вместе со всеми номерами. Доступно только администратору.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Отель удалён"),
                    @ApiResponse(responseCode = "404", description = "Отель не найден",
                            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
            })
    ResponseEntity<Void> deleteHotel(
            @Parameter(description = "Идентификатор отеля") @PathVariable UUID hotelId);
}
