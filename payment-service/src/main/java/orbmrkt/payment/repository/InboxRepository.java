package orbmrkt.payment.repository;

import orbmrkt.payment.model.InboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<InboxEntity, UUID> {

    boolean existsByEventId(UUID eventId);

    void deleteByProcessedAtBefore(Instant cutoff);
}
