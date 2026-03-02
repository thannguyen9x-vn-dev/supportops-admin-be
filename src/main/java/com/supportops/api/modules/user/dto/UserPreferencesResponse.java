package com.supportops.api.modules.user.dto;

public record UserPreferencesResponse(
    boolean companyNews,
    boolean accountActivity,
    boolean meetupsNearYou,
    boolean newMessages,
    boolean ratingReminders,
    boolean itemUpdateNotif,
    boolean itemCommentNotif,
    boolean buyerReviewNotif
) {}
