package com.hrms.backend.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrms.backend.dto.NotificationDto;
import com.hrms.backend.entities.Notification;
import com.hrms.backend.repository.NotificationRepository;
import com.hrms.backend.repository.UserNotificationRepository;
import com.hrms.backend.repository.UserRepository;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SocketServer extends WebSocketServer {
    HashMap<String, WebSocket> userConnections = new HashMap();
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    LocalDateTime latestNotificationTime = LocalDateTime.now();
    List<Notification> notificationList = new ArrayList<>();
    long totalUserNotificationCount = 0;
    public SocketServer(InetSocketAddress address, NotificationRepository notificationRepository, UserNotificationRepository userNotificationRepository, UserRepository userRepository) {
        super(address);
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        String url = clientHandshake.getResourceDescriptor(); // This gives us the full URL path and query parameters
        System.out.println("Connected to " + url);
        String userId = null;
        String role = null;
        fetchData();
        // Manually parse the query parameters from the URL
        if (url != null && url.contains("?")) {
            String query = url.split("\\?")[1];

            String[] params = query.split("&");

            for (int i = 0; i < params.length; i++) {
                String[] keyValue = params[i].split("=");
                if (keyValue.length == 2 && "userId".equals(keyValue[0])) {
                    userId = keyValue[1];
                }
                if (keyValue.length == 2 && "role".equals(keyValue[0])) {
                    role = keyValue[1];
                }
            }
        }
        System.out.println("Connected to " + userId);
        if (userId != null) {
            // Store the WebSocket connection with the userId
            //userConnections.put(userId, webSocket);
            userConnections.put(userId + "|" + role, webSocket);
            System.out.println("User connected: " + userId);
            try {
                sendList(webSocket,userId + "|" + role);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No userId provided in the URL");
        }
        System.out.println("OPENED");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println("CLOSED");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println("RECEIVED" + s);
        //this.broadcast(s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("ERROR:" + e.getMessage());
    }

    @Override
    public void onStart() {
        dataPool();
        System.out.println("WEBSOCKET STARTED");
    }

    public void dataPool() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Define the task
        Runnable task = this::fetchData;

        // Schedule the task to run every second
        scheduler.scheduleAtFixedRate(task, 0, 3, TimeUnit.SECONDS);
    }

    private void fetchData() {
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt")));
        // Fetch data using pagination and sorting
        Page<Notification> page = notificationRepository.findAll(pageRequest);
        long count = userNotificationRepository.count();
        System.out.println("===="+(count!=totalUserNotificationCount));
        if((!page.getContent().get(0).getCreatedAt().equals(latestNotificationTime)) || count != totalUserNotificationCount){
            pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Order.desc("createdAt")));
            page = notificationRepository.findAll(pageRequest);
            notificationList = page.getContent();
            totalUserNotificationCount = count;
            latestNotificationTime = notificationList.get(0).getCreatedAt();
            userConnections.keySet().forEach(key->{
                if(!userConnections.get(key).isClosed()) {
                    try {
                        sendList(userConnections.get(key),key);
                    } catch (JsonProcessingException e) {
                       e.printStackTrace();
                    }
                }
            });
        }
    }

    private void sendList(WebSocket webSocket,String key) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        List<Notification> filteredNotificationList = new ArrayList<>();
        List<NotificationDto> filteredNotificationDtoList = new ArrayList<>();
        int userId = Integer.parseInt(key.split("\\|")[0]);
        filteredNotificationList.addAll(notificationList.stream().filter(a->a.getNotificationGroup().contains(key.split("\\|")[1].toUpperCase())).toList());
        filteredNotificationList.addAll(notificationList.stream().filter(a->a.getUser().getId() == userId && !filteredNotificationList.contains(a)).toList());

        filteredNotificationList.forEach(notification -> {
            if (userNotificationRepository.findUserNotificationByUserAndNotification(userRepository.findById(Math.toIntExact(userId)).get(), notificationRepository.findById(notification.getId()).get()) == null){
                filteredNotificationDtoList.add(new NotificationDto(notification.getId(),notification.getMessage(),false));
            }else{
                filteredNotificationDtoList.add(new NotificationDto(notification.getId(),notification.getMessage(),true));
            }
        });
        webSocket.send(objectMapper.writeValueAsString(filteredNotificationDtoList.stream()
                .sorted(Comparator.comparing(NotificationDto::getId))
                .collect(Collectors.toList())));
    }

}
