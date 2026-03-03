package com.supportops.api.modules.user.dto;

public record UpdateUserPreferencesRequest(
    Boolean companyNews,
    Boolean accountActivity,
    Boolean meetupsNearYou,
    Boolean newMessages,
    Boolean ratingReminders,
    Boolean itemUpdateNotif,
    Boolean itemCommentNotif,
    Boolean buyerReviewNotif
) {}
