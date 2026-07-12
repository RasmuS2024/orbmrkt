package orbmrkt.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalErrorHandler handler = new GlobalErrorHandler(objectMapper);

    private final ServerWebExchange exchange = mock();
    private final ServerHttpResponse response = mock();
    private final HttpHeaders headers = mock();
    private final DataBufferFactory bufferFactory = mock();
    private final DataBuffer dataBuffer = mock();

    @BeforeEach
    void setUp() {
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    @Test
    void notFoundException_returns404() {
        handler.handle(exchange, new NotFoundException("not found"));
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    void connectException_returns502() {
        handler.handle(exchange, new ConnectException("connection refused"));
        verify(response).setStatusCode(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void connectTimeoutException_returns502() {
        handler.handle(exchange, new ConnectTimeoutException("connect timed out"));
        verify(response).setStatusCode(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void timeoutException_returns504() {
        handler.handle(exchange, new TimeoutException("timed out"));
        verify(response).setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void readTimeoutException_returns504() {
        handler.handle(exchange, ReadTimeoutException.INSTANCE);
        verify(response).setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void webClientResponseException_passthroughStatusCode() {
        var webClientEx = WebClientResponseException.create(400, "Bad Request", null, null, null);
        handler.handle(exchange, webClientEx);
        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
    }

    @Test
    void responseStatusException_passthroughStatusCode() {
        var statusEx = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found");
        handler.handle(exchange, statusEx);
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    void genericException_returns500() {
        handler.handle(exchange, new RuntimeException("unexpected"));
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
