package com.sign.sign.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateRoomRequest {
    private Long creatorId;
    private Long guestId;
}
