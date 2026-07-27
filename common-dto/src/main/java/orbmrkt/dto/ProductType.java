package orbmrkt.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип продукта спутниковой съёмки", enumAsRef = true)
public enum ProductType {
    ARCHIVE,
    TASKING,
    MONITORING;

    @JsonCreator
    public static ProductType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ProductType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
