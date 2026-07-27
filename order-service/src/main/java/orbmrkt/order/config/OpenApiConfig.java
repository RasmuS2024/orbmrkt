package orbmrkt.order.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Order Service API", version = "1.0"),
    servers = @Server(url = "http://localhost:8080"),
    security = @SecurityRequirement(name = "X-User-Id")
)
@SecurityScheme(
    name = "X-User-Id",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-User-Id",
    description = "UUID пользователя (для локальной разработки – любой валидный UUID)"
)
public class OpenApiConfig {
}
