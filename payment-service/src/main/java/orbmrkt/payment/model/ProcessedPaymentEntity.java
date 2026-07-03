package orbmrkt.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_payments")
public class ProcessedPaymentEntity {

    @Id
    private UUID orderId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        this.processedAt = Instant.now();
    }
}
