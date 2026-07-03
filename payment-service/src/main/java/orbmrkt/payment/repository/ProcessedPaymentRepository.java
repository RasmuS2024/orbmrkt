package orbmrkt.payment.repository;

import orbmrkt.payment.model.ProcessedPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPaymentEntity, UUID> {
    boolean existsByOrderId(UUID orderId);
}
