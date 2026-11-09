package org.mephi.hotelbooking.hotel.rest.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mephi.hotelbooking.hotel.config.TraceIdFilter;
import org.mephi.hotelbooking.hotel.exception.BookingConflictException;
import org.mephi.hotelbooking.hotel.exception.EntityNotFoundException;
import org.mephi.hotelbooking.hotel.exception.InvalidDateRangeException;
import org.mephi.hotelbooking.hotel.exception.RoomNotAvailableException;
import org.mephi.hotelbooking.hotel.rest.dto.ErrorDto;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Ресурс не найден: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ErrorDto> handleRoomNotAvailable(RoomNotAvailableException ex, HttpServletRequest request) {
        log.warn("Номер недоступен: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorDto> handleConflict(BookingConflictException ex, HttpServletRequest request) {
        log.warn("Конфликт бронирования: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ErrorDto> handleInvalidDates(InvalidDateRangeException ex, HttpServletRequest request) {
        log.warn("Некорректный диапазон дат: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Недопустимое значение: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Ошибка валидации: {}", errors);

        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации запроса", request, errors);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorDto> handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("Доступ запрещён: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Недостаточно прав для выполнения операции", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Неизвестная ошибка: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла непредвиденная ошибка.", request, null);
    }

    private ResponseEntity<ErrorDto> build(HttpStatus status, String message,
                                           HttpServletRequest request, Map<String, String> details) {
        ErrorDto body = ErrorDto.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .traceId(MDC.get(TraceIdFilter.MDC_KEY))
                .details(details)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
