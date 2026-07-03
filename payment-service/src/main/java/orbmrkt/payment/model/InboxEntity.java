package orbmrkt.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "inbox")
public class InboxEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Instant processedAt;

    public InboxEntity(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
