package com.stocklab.core.api.ws;

import com.stocklab.core.domain.matching.Execution;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Execution execution) {
        ExecutionMessage message = ExecutionMessage.from(execution);
        String stockCode = execution.getStock().getStockCode();

        messagingTemplate.convertAndSend("/topic/stocks/" + stockCode + "/executions", message);
        messagingTemplate.convertAndSend("/topic/users/" + execution.getBuyer().getId() + "/executions", message);
        messagingTemplate.convertAndSend("/topic/users/" + execution.getSeller().getId() + "/executions", message);
    }
}
