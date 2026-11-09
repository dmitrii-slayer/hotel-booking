package org.mephi.hotelbooking.booking.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Страница результатов")
public class PageResponse<T> {

    @Schema(description = "Содержимое страницы")
    private List<T> content;

    @Schema(description = "Номер страницы (с нуля)", example = "0")
    private int page;

    @Schema(description = "Размер страницы", example = "20")
    private int size;

    @Schema(description = "Всего элементов", example = "42")
    private long totalElements;

    @Schema(description = "Всего страниц", example = "3")
    private int totalPages;

    @Schema(description = "Признак последней страницы", example = "false")
    private boolean last;
}
