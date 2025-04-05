package com.hrms.backend.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import com.hrms.backend.entities.Request;

@Component
public class RequestListener {

   /* @Autowired
    private SimpMessagingTemplate template;

    @JmsListener(destination = "request.queue")
    public void receiveMessage(Request request) {
        template.convertAndSend("/topic/requests", request);
    }*/
}
