package com.sba.ssos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Paginated response payload")
public record PageResponse<T>(
    @Schema(description = "Page content")
    List<T> content,
    @Schema(description = "Current page index")
    int number,
    @Schema(description = "Requested page size")
    int size,
    @Schema(description = "Total number of matching records")
    long totalElements,
    @Schema(description = "Total number of pages")
    int totalPages,
    @Schema(description = "Whether this is the first page")
    boolean first,
    @Schema(description = "Whether this is the last page")
    boolean last) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }
}
