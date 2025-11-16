package com.example.demo.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Component
@Slf4j
public class SolicitudEventsStore {

    private static final int MAX_EVENTS = 500;
    private final Deque<SolicitudEventsPublisher.WsEvent> events = new LinkedList<>();

    public synchronized void append(SolicitudEventsPublisher.WsEvent event) {
        events.addLast(event);
        if (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }

    public synchronized List<SolicitudEventsPublisher.WsEvent> listAll() {
        return List.copyOf(events);
    }
}
