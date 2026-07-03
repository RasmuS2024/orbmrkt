package orbmrkt.order.repository;

import orbmrkt.order.model.OutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findByProcessedFalseAndAttemptsLessThanAndNextRetryAtBefore(
            int maxAttempts, Instant now, Pageable pageable);

    List<OutboxEntity> findByProcessedFalseAndAttemptsLessThanAndNextRetryAtIsNull(
            int maxAttempts, Pageable pageable);

    void deleteByProcessedTrueAndCreatedAtBefore(Instant cutoff);
}
