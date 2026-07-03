package orbmrkt.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.dto.OrderStatus;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.ArchivePayload;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.exception.OrderException;
import orbmrkt.order.messaging.OrderEventPublisher;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "MISSING_USER_ID", "Требуется User ID");
        }

        validateProductType(request.getProductType());
        validatePrice(request.getPrice());
        validatePayload(request.getProductType(), request.getPayload());

        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setProductType(request.getProductType().name());
        order.setPrice(request.getPrice());
        order.setStatus(OrderStatus.CREATED.name());
        order.setPayload(serializePayload(request.getPayload()));
        order = repository.save(order);

        OrderPaymentRequested event = new OrderPaymentRequested();
        event.setOrderId(order.getId());
        event.setAmount(order.getPrice());
        event.setUserId(userId);
        eventPublisher.publishPaymentRequested(event);

        order.setStatus(OrderStatus.PAYMENT_PENDING.name());
        order = repository.save(order);

        return toResponse(order);
    }

    public OrderResponse getOrder(UUID orderId, String userId) {
        OrderEntity order = repository.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new OrderException(
                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Заказ не найден"));
        return toResponse(order);
    }

    public List<OrderResponse> listOrders(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateProductType(ProductType productType) {
        if (productType == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "UNKNOWN_PRODUCT_TYPE",
                    "Требуется тип продукта");
        }
    }

    private void validatePrice(java.math.BigDecimal price) {
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PRICE",
                    "Цена должна быть больше 0");
        }
    }

    private void validatePayload(ProductType productType, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                    "Требуется payload");
        }
        if (productType == ProductType.ARCHIVE) {
            ArchivePayload archive = objectMapper.convertValue(payload, ArchivePayload.class);
            if (isBlank(archive.getAreaOfInterest()) || isBlank(archive.getCaptureDate())
                    || isBlank(archive.getSensorType())) {
                throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                        "Для ARCHIVE требуются area_of_interest, capture_date, sensor_type");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                    "Не удалось сериализовать payload");
        }
    }

    private OrderResponse toResponse(OrderEntity order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setStatus(order.getStatus());
        response.setProductType(order.getProductType());
        response.setPrice(order.getPrice());
        response.setCreatedAt(order.getCreatedAt());
        response.setFailureReason(order.getFailureReason());
        return response;
    }
}
