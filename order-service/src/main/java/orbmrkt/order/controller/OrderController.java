package orbmrkt.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import orbmrkt.dto.UserId;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@Tag(name = "Orders", description = "Управление заказами на спутниковую съёмку и мониторинг")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @Operation(summary = "Создать заказ", description = "Создаёт новый заказ на спутниковую съёмку. " +
            "Инициирует процесс платежа. Тип продукта определяет структуру payload.")
    @ApiResponse(responseCode = "201", description = "Заказ создан",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "INVALID_PAYLOAD, INVALID_PRICE, "
            + "UNKNOWN_PRODUCT_TYPE или MISSING_USER_ID - невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId,
            @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    @Operation(summary = "Список заказов", description = "Возвращает все заказы для указанного пользователя")
    @ApiResponse(responseCode = "200", description = "Список заказов пользователя",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "MISSING_USER_ID - отсутствует или невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId) {
        return ResponseEntity.ok(orderService.listOrders(userId));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Детали заказа", description = "Возвращает информацию о конкретном заказе")
    @ApiResponse(responseCode = "200", description = "Информация о заказе",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "MISSING_USER_ID - отсутствует или невалидный X-User-Id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND - заказ не найден или чужой user_id",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR - внутренняя ошибка сервера",
            content = @Content(schema = @Schema(implementation = orbmrkt.dto.ApiResponse.class)))
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }
}
