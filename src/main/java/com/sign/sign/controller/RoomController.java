package com.sign.sign.controller;

import com.sign.sign.domain.Room;
import com.sign.sign.service.RoomService;
import com.sign.sign.dto.request.CreateRoomRequest;
import com.sign.sign.dto.response.CreateRoomResponse;
import com.sign.sign.dto.response.RoomInfoResponse;
import com.sign.sign.dto.response.DeleteRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request) {
        Long roomId = roomService.createRoom(request.getCreatorId(), request.getGuestId());
        Room room = roomService.findById(roomId);

        return ResponseEntity.ok(new CreateRoomResponse(room.getRoomId(), roomId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId) {
        try {
            Room room = roomService.findByRoomId(roomId);
            return ResponseEntity.ok(new RoomInfoResponse(
                    room.getId(),
                    room.getRoomId(),
                    room.getCreator().getName(),
                    room.getGuest().getName(),
                    room.isActive()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable String roomId) {
        try {
            Long deletedRoomId = roomService.remove(roomId);
            return ResponseEntity.ok(new DeleteRoomResponse(true, "방이 삭제되었습니다", deletedRoomId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new DeleteRoomResponse(false, e.getMessage(), null));
        }
    }
}