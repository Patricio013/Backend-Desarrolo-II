package com.example.demo.repository;

import com.example.demo.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long>, JpaSpecificationExecutor<WebhookEvent> {
}
