package com.internship.tool.repository;

import com.internship.tool.entity.NotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    boolean existsByDedupeKey(String dedupeKey);
    long countByStatus(NotificationOutbox.DeliveryStatus status);
    Optional<NotificationOutbox> findFirstByStatusOrderByCreatedAtAsc(NotificationOutbox.DeliveryStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificationOutbox n where n.status = :status and n.availableAt <= :now order by n.id")
    List<NotificationOutbox> lockReady(@Param("now") Instant now,
                                       @Param("status") NotificationOutbox.DeliveryStatus status,
                                       Pageable page);
}
