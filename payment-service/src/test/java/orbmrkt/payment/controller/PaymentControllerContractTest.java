package orbmrkt.payment.controller;

import orbmrkt.payment.exception.GlobalExceptionHandler;
import orbmrkt.payment.exception.PaymentException;
import orbmrkt.payment.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createAccount_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/accounts")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void topUp_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                        .contentType("application/json")
                        .content("{\"amount\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void getBalance_missingUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/payments/accounts/balance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void createAccount_paymentException_passesStatusAndCode() throws Exception {
        when(accountService.createAccount(anyString()))
                .thenThrow(new PaymentException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Not found"));

        mockMvc.perform(post("/api/v1/payments/accounts")
                        .header("X-User-Id", "user")
                        .contentType("application/json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void topUp_optimisticLock_returns409() throws Exception {
        when(accountService.topUp(anyString(), anyLong()))
                .thenThrow(new OptimisticLockingFailureException("lock conflict"));

        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{\"amount\": 100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("OPTIMISTIC_LOCK_FAILURE"));
    }

    @Test
    void topUp_dataIntegrityViolation_returns409() throws Exception {
        when(accountService.topUp(anyString(), anyLong()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{\"amount\": 100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void getBalance_runtimeException_returns500() throws Exception {
        when(accountService.getBalance(anyString()))
                .thenThrow(new RuntimeException("unexpected error"));

        mockMvc.perform(get("/api/v1/payments/accounts/balance")
                        .header("X-User-Id", "user"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_code").value("INTERNAL_ERROR"));
    }

    @Test
    void topUp_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                        .header("X-User-Id", "user")
                        .contentType("application/json")
                        .content("{malformed}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_JSON"));
    }

    @Test
    void unknownPath_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/nonexistent")
                        .header("X-User-Id", "user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"));
    }
}
