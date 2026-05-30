package ch.insurtech.platform.shared.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorCode,
        String message,
        List<String> violations,
        Instant timestamp
) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, null, Instant.now());
    }

    public static ErrorResponse of(String errorCode, String message, List<String> violations) {
        return new ErrorResponse(errorCode, message, violations, Instant.now());
    }
}
