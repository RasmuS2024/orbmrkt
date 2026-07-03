package orbmrkt.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import orbmrkt.dto.ProductType;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class CreateOrderRequest {

    @JsonProperty("product_type")
    private ProductType productType;

    private BigDecimal price;

    private Map<String, Object> payload;
}
