package com.fanzzi.backend.post.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PollOption {

    // =====================================
    // OPTION ID
    // =====================================
    private String optionId;

    // =====================================
    // TEXT
    // =====================================
    private String text;

    // =====================================
    // VOTES
    // =====================================
    private long votes;

    // =====================================
    // PERCENTAGE (UI READY)
    // =====================================
    private double percentage;

    // =====================================
    // USER STATE
    // =====================================
    private boolean selected;

    // =====================================
    // QUIZ MODE
    // =====================================
    private boolean correct;

    // =====================================
    // ORDER (OPTIONAL)
    // =====================================
    private int order;
}