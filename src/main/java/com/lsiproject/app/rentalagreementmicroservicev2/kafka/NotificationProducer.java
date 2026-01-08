package com.lsiproject.app.rentalagreementmicroservicev2.kafka;

import com.lsiproject.app.rentalagreementmicroservicev2.configuration.KafkaProducerConfig;
import com.lsiproject.app.rentalagreementmicroservicev2.dtos.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final KafkaProducerConfig kafkaProducerConfig;

    public void sendNotification(NotificationEvent event, String topic) {
        kafkaTemplate.send(topic, kafkaProducerConfig.serialize(event));
    }
}
