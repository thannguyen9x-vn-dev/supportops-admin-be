package com.supportops.api.modules.user.entity;

import com.supportops.api.common.tenant.TenantAware;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_preferences")
public class UserPreference implements TenantAware {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_news", nullable = false)
    private boolean companyNews = false;

    @Column(name = "account_activity", nullable = false)
    private boolean accountActivity = true;

    @Column(name = "meetups_near_you", nullable = false)
    private boolean meetupsNearYou = true;

    @Column(name = "new_messages", nullable = false)
    private boolean newMessages = false;

    @Column(name = "rating_reminders", nullable = false)
    private boolean ratingReminders = true;

    @Column(name = "item_update_notif", nullable = false)
    private boolean itemUpdateNotif = true;

    @Column(name = "item_comment_notif", nullable = false)
    private boolean itemCommentNotif = true;

    @Column(name = "buyer_review_notif", nullable = false)
    private boolean buyerReviewNotif = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
