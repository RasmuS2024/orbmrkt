package orbmrkt.order.controller;

import orbmrkt.order.exception.GlobalExceptionHandler;
import orbmrkt.order.exception.OrderException;
import orbmrkt.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders/orders")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void createOrder_orderException_passesStatusAndCode() throws Exception {
        when(orderService.createOrder(anyString(), any()))
                .thenThrow(new OrderException(HttpStatus.BAD_REQUEST, "INVALID_PRICE", "Price must be > 0"));

        mockMvc.perform(post("/api/v1/orders/orders")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{\"productType\": \"ARCHIVE\", \"price\": 500, \"payload\": {}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_PRICE"));
    }

    @Test
    void getOrder_orderException_returns404() throws Exception {
        when(orderService.getOrder(any(), anyString()))
                .thenThrow(new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Not found"));

        mockMvc.perform(get("/api/v1/orders/orders/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("X-User-Id", "user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void createOrder_runtimeException_returns500() throws Exception {
        when(orderService.createOrder(anyString(), any()))
                .thenThrow(new RuntimeException("unexpected error"));

        mockMvc.perform(post("/api/v1/orders/orders")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{\"productType\": \"ARCHIVE\", \"price\": 500, \"payload\": {}}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_code").value("INTERNAL_ERROR"));
    }

    @Test
    void createOrder_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders/orders")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{broken}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_JSON"));
    }

    @Test
    void unknownPath_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/nonexistent")
                        .header("X-User-Id", "user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"));
    }
}
