package orbmrkt.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private long pollingIntervalMs = 500;
    private int batchSize = 100;
    private int maxAttempts = 5;
    private int cleanupDays = 7;
}
