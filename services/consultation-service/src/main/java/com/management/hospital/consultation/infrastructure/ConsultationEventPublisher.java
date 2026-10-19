package com.management.hospital.consultation.infrastructure;

import com.management.hospital.common.event.ConsultationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConsultationEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ConsultationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishConsultationCompleted(ConsultationCompletedEvent event) {
        log.info("Publishing ConsultationCompletedEvent for consultationId: {}, appointmentId: {}",
                event.consultationId(), event.appointmentId());
        kafkaTemplate.send(ConsultationCompletedEvent.TOPIC, event.consultationId().toString(), event);
    }
}
