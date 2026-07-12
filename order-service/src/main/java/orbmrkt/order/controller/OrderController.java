package orbmrkt.order.controller;

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
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId,
            @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId) {
        return ResponseEntity.ok(orderService.listOrders(userId));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader("X-User-Id")
            @Pattern(regexp = UserId.UUID_REGEX)
            String userId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId, userId));
    }
}
