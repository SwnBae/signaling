package com.sign.sign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteRoomResponse {
    private boolean success;
    private String message;
    private Long deletedRoomId;
}
