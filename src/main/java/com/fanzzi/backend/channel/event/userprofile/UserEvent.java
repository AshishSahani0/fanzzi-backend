package com.fanzzi.backend.channel.event.userprofile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserEvent {

    private String userId;
    private UserEventType type;
    private Object payload;

}
