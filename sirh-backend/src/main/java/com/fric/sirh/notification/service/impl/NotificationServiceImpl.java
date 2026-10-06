package com.fric.sirh.notification.service.impl;

import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.dto.NotificationDto;
import com.fric.sirh.notification.dto.NotificationFilter;
import com.fric.sirh.notification.dto.NotificationPageResponse;
import com.fric.sirh.notification.entity.Notification;
import com.fric.sirh.exception.NotificationNotFoundException;
import com.fric.sirh.notification.exception.UnauthorizedAccessException;
import com.fric.sirh.notification.mapper.NotificationMapper;
import com.fric.sirh.notification.repository.NotificationRepository;
import com.fric.sirh.notification.service.NotificationService;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public Notification save(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(notification);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }
        notifications.forEach(n -> {
            if (n.getCreatedAt() == null) {
                n.setCreatedAt(LocalDateTime.now());
            }
        });
        return repository.saveAll(notifications);
    }

    @Override
    public NotificationPageResponse getNotifications(NotificationFilter filter, Pageable pageable, String currentUserId) {
        Query query = buildFilterQuery(filter);
        query.with(pageable);
        long total = mongoTemplate.count(query, Notification.class);
        List<Notification> list = mongoTemplate.find(query, Notification.class);
        Page<Notification> page = new PageImpl<>(list, pageable, total);
        Page<NotificationDto> dtoPage = page.map(mapper::toDto);
        return NotificationPageResponse.fromPage(dtoPage);
    }

    @Override
    public long countUnread(String userId) {
        return repository.countUnreadByRecipientId(userId);
    }

    @Override
    public NotificationPageResponse getUnread(String userId, Pageable pageable) {
        Page<Notification> page = repository.findUnreadByRecipientId(userId, pageable);
        Page<NotificationDto> dtoPage = page.map(mapper::toDto);
        return NotificationPageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId, String currentUserId) {
        Notification notification = findAndCheckAccess(notificationId, currentUserId);
        notification.setRead(true);
        repository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        Query query = new Query(Criteria.where("recipientId").is(userId).and("read").is(false));
        Update update = new Update().set("read", true);
        mongoTemplate.updateMulti(query, update, Notification.class);
    }

    @Override
    @Transactional
    public void delete(String notificationId, String currentUserId) {
        Notification notification = findAndCheckAccess(notificationId, currentUserId);
        repository.delete(notification);
    }

    @Override
    @Transactional
    public void deleteAll(String userId) {
        Query query = new Query(Criteria.where("recipientId").is(userId));
        mongoTemplate.remove(query, Notification.class);
    }

    @Override
    public NotificationPageResponse search(NotificationFilter filter, Pageable pageable, String currentUserId) {
        return getNotifications(filter, pageable, currentUserId);
    }

    @Override
    public NotificationPageResponse getHistory(String userId, NotificationFilter filter, Pageable pageable) {
        if (filter == null) {
            filter = new NotificationFilter();
        }
        filter.setRecipientId(userId);
        return getNotifications(filter, pageable, userId);
    }

    // ========== MÉTHODES PRIVÉES ==========

    private Notification findAndCheckAccess(String notificationId, String userId) {
        Optional<Notification> optional = repository.findById(notificationId);
        if (optional.isEmpty()) {
            throw new NotificationNotFoundException("Notification not found with id: " + notificationId);
        }
        Notification notification = optional.get();

        // Autoriser si l'utilisateur est le destinataire
        if (notification.getRecipientId().equals(userId)) {
            return notification;
        }

        // Sinon, vérifier le rôle (RH ou TOP_MANAGER ont accès)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + user.getRoleId()));

        String roleName = role.getName();
        if ("RH".equals(roleName) || "TOP_MANAGER".equals(roleName)) {
            return notification; // ✅ Accès autorisé
        }

        throw new UnauthorizedAccessException("You are not allowed to access this notification");
    }

    private Query buildFilterQuery(NotificationFilter filter) {
        Query query = new Query();
        Criteria criteria = new Criteria();

        if (filter != null) {
            if (filter.getRecipientId() != null) {
                criteria.and("recipientId").is(filter.getRecipientId());
            }
            if (filter.getCompanyId() != null) {
                criteria.and("companyId").is(filter.getCompanyId());
            }
            if (filter.getDepartmentId() != null) {
                criteria.and("departmentId").is(filter.getDepartmentId());
            }
            if (filter.getRead() != null) {
                criteria.and("read").is(filter.getRead());
            }
            if (filter.getEvent() != null) {
                criteria.and("event").is(filter.getEvent());
            }
            if (filter.getType() != null) {
                criteria.and("type").is(filter.getType());
            }
            if (filter.getPriority() != null) {
                criteria.and("priority").is(filter.getPriority());
            }
            if (filter.getFromDate() != null && filter.getToDate() != null) {
                criteria.and("createdAt").gte(filter.getFromDate()).lte(filter.getToDate());
            } else if (filter.getFromDate() != null) {
                criteria.and("createdAt").gte(filter.getFromDate());
            } else if (filter.getToDate() != null) {
                criteria.and("createdAt").lte(filter.getToDate());
            }
        }

        query.addCriteria(criteria);
        return query;
    }
}