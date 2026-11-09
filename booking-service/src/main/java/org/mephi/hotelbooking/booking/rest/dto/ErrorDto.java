package org.mephi.hotelbooking.booking.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Единый формат ошибки API")
public class ErrorDto {

    @Schema(description = "Момент возникновения ошибки", example = "2026-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP-статус", example = "409")
    private Integer status;

    @Schema(description = "Краткое название статуса", example = "Conflict")
    private String error;

    @Schema(description = "Человекочитаемое описание", example = "Номер занят на запрошенные даты")
    private String message;

    @Schema(description = "URI запроса", example = "/api/booking")
    private String path;

    @Schema(description = "Идентификатор трассировки запроса", example = "a1b2c3d4e5f6")
    private String traceId;

    @Schema(description = "Ошибки валидации: поле - сообщение")
    private Map<String, String> details;
}
