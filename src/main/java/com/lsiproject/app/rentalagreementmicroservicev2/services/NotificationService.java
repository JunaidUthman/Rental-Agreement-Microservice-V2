package com.lsiproject.app.rentalagreementmicroservicev2.services;

import com.lsiproject.app.rentalagreementmicroservicev2.dtos.NotificationEvent;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.Channel;
import com.lsiproject.app.rentalagreementmicroservicev2.enums.EventType;
import com.lsiproject.app.rentalagreementmicroservicev2.kafka.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer producer;
    private final String topic = "notification-events";

    public void notify(EventType eventType, List<Long> userIds, String title, String message, Map<String, Object> metadata) {
        NotificationEvent event = NotificationEvent.builder()
                .eventType(eventType)
                .userIds(userIds)
                .title(title)
                .message(message)
                .channels(List.of(Channel.PUSH))
                .metadata(metadata)
                .build();

        producer.sendNotification(event, topic);
    }
}
