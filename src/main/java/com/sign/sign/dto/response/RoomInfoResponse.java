package com.sign.sign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomInfoResponse {
    private Long id;
    private String roomId;
    private String creatorName;
    private String guestName;
    private boolean isActive;
}
