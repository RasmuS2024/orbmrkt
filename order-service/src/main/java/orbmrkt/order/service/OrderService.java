package orbmrkt.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import orbmrkt.dto.OrderPaymentRequested;
import orbmrkt.dto.OrderStatus;
import orbmrkt.dto.ProductType;
import orbmrkt.order.dto.ArchivePayload;
import orbmrkt.order.dto.CreateOrderRequest;
import orbmrkt.order.dto.MonitoringPayload;
import orbmrkt.order.dto.OrderResponse;
import orbmrkt.order.dto.TaskingPayload;
import orbmrkt.order.dto.TimeWindow;
import orbmrkt.order.exception.OrderException;
import orbmrkt.order.messaging.OrderEventPublisher;
import orbmrkt.order.model.OrderEntity;
import orbmrkt.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
            throw new OrderException(HttpStatus.BAD_REQUEST, "MISSING_USER_ID", "User ID is required");
        }

        String validationError = validateOrder(request);
        if (validationError != null) {
            OrderEntity order = new OrderEntity();
            order.setUserId(userId);
            order.setProductType(request.getProductType() != null
                    ? request.getProductType().name() : "UNKNOWN");
            order.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
            order.setPayload(request.getPayload() != null
                    ? serializePayload(request.getPayload()) : "{}");
            order.setStatus(OrderStatus.REJECTED.name());
            order.setFailureReason(validationError);
            repository.save(order);
            String msg = switch (validationError) {
                case "INVALID_PRICE" -> "Price must be greater than 0";
                case "UNKNOWN_PRODUCT_TYPE" -> "Unsupported product type";
                default -> "Missing required fields in payload";
            };
            throw new OrderException(HttpStatus.BAD_REQUEST, validationError, msg);
        }

        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setProductType(request.getProductType().name());
        order.setPrice(request.getPrice());
        order.setPayload(request.getPayload() != null ? serializePayload(request.getPayload()) : "{}");
        order.setStatus(OrderStatus.CREATED.name());
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
                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        return toResponse(order);
    }

    public List<OrderResponse> listOrders(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String validateOrder(CreateOrderRequest request) {
        if (request.getProductType() == null) {
            return "UNKNOWN_PRODUCT_TYPE";
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "INVALID_PRICE";
        }
        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            return "INVALID_PAYLOAD";
        }
        if (request.getProductType() == ProductType.ARCHIVE) {
            ArchivePayload archive = objectMapper.convertValue(request.getPayload(), ArchivePayload.class);
            if (isBlank(archive.getAoi()) || isBlank(archive.getCaptureDate())
                    || isBlank(archive.getSensorType())) {
                return "INVALID_PAYLOAD";
            }
        } else if (request.getProductType() == ProductType.TASKING) {
            TaskingPayload tasking = objectMapper.convertValue(request.getPayload(), TaskingPayload.class);
            TimeWindow tw = tasking.getTimeWindow();
            if (isBlank(tasking.getAoi()) || tw == null
                    || isBlank(tw.getFrom()) || isBlank(tw.getTo())
                    || isBlank(tasking.getSensorType())) {
                return "INVALID_PAYLOAD";
            }
        } else if (request.getProductType() == ProductType.MONITORING) {
            MonitoringPayload monitoring = objectMapper.convertValue(request.getPayload(), MonitoringPayload.class);
            if (isBlank(monitoring.getAoi()) || isBlank(monitoring.getCadence())
                    || monitoring.getDurationDays() <= 0) {
                return "INVALID_PAYLOAD";
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                    "Failed to serialize payload");
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
