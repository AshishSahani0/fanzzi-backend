package com.fanzzi.backend.live.premium.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document("creator_live_plans")
public class CreatorLivePlan {

    @Id
    private String id;

    private String ownerId;

    private int totalMinutes;
    private int usedMinutes;

    private boolean active;



}