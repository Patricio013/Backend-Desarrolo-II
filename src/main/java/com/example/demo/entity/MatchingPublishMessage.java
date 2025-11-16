package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "matching_publish_message", indexes = {
        @Index(name = "ix_publish_status", columnList = "status"),
        @Index(name = "ix_publish_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingPublishMessage {

    private static final ZoneId ARG_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 80)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PublishStatus status = PublishStatus.PENDING;

    @Column(name = "destination_topic", nullable = false, length = 80)
    private String destinationTopic;

    @Column(name = "destination_event", nullable = false, length = 80)
    private String destinationEvent;

    @Column(name = "message_timestamp", nullable = false, length = 40)
    private String messageTimestamp;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public enum PublishStatus {
        PENDING,
        SENT,
        FAILED
    }

    public enum MessageType {
        TOP3,
        COTIZACIONES,
        SOLICITUD_PAGO
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PublishStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markAttempt(Integer statusCode, String error, PublishStatus newStatus) {
        attempts = attempts + 1;
        lastAttemptAt = LocalDateTime.now();
        lastStatusCode = statusCode;
        lastError = error;
        status = newStatus;
    }

    @Transient
    @JsonProperty("messageTimestampArgentina")
    public String getMessageTimestampArgentina() {
        if (messageTimestamp == null) {
            return null;
        }
        try {
            Instant instant = Instant.parse(messageTimestamp);
            return instant.atZone(ARG_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return messageTimestamp;
        }
    }
}
