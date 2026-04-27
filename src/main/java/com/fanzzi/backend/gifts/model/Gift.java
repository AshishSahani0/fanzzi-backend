package com.fanzzi.backend.gifts.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("gifts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gift {

    @Id
    private String id;

    private String name;
    private String emoji;
    private int price;

    private String animation;

    private boolean active;
    private boolean seasonal;

    private long createdAt;
}