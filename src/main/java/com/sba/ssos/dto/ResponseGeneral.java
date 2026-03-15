package com.sba.ssos.dto;

import com.sba.ssos.utils.DateUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Schema(description = "Standard API response wrapper")
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ResponseGeneral<T> {
    @Schema(description = "HTTP status code")
    private int status;
    @Schema(description = "Human-readable response message")
    private String message;
    @Schema(description = "Payload for the request")
    private T data;
    @Schema(description = "Response creation timestamp")
    private String timestamp;

    public static <T> ResponseGeneral<T> of(int status, String message, T data) {
        return of(status, message, data, DateUtils.getCurrentDateString());
    }

    public static <T> ResponseGeneral<T> ofSuccess(String message, T data) {
        return of(HttpStatus.OK.value(), message, data, DateUtils.getCurrentDateString());
    }

    public static <T> ResponseGeneral<T> ofSuccess(String message) {
        return of(HttpStatus.OK.value(), message, null, DateUtils.getCurrentDateString());
    }

    public static <T> ResponseGeneral<T> ofCreated(String message, T data) {
        return of(HttpStatus.CREATED.value(), message, data, DateUtils.getCurrentDateString());
    }

}
