package orbmrkt.payment.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTestUtils {

    private final KafkaTemplate<String, Object> kafka;

    public void send(String topic, Object event) {
        kafka.send(topic, event).join();
    }

    public void send(String topic, String key, Object event) {
        kafka.send(topic, key, event).join();
    }
}
