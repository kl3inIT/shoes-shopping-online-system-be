package com.sba.ssos.repository;

import com.sba.ssos.entity.Notification;
import com.sba.ssos.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByType(NotificationType type);
}

