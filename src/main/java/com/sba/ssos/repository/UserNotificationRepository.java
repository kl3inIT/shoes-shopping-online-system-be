package com.sba.ssos.repository;

import com.sba.ssos.entity.UserNotification;
import com.sba.ssos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUser(User user);
}

