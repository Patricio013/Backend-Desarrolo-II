package com.example.demo.repository;

import com.example.demo.entity.MatchingPublishMessage;
import com.example.demo.entity.MatchingPublishMessage.PublishStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MatchingPublishMessageRepository extends JpaRepository<MatchingPublishMessage, Long> {

    @Query("SELECT m FROM MatchingPublishMessage m WHERE m.status IN :statuses ORDER BY m.createdAt ASC")
    List<MatchingPublishMessage> findByStatusIn(@Param("statuses") Collection<PublishStatus> statuses, Pageable pageable);

    long countByStatusIn(Collection<PublishStatus> statuses);

    List<MatchingPublishMessage> findByIdIn(Collection<Long> ids);
}
