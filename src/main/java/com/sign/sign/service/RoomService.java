package com.sign.sign.service;

import com.sign.sign.domain.Member;
import com.sign.sign.domain.Room;
import com.sign.sign.repository.MemberRepository;
import com.sign.sign.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long createRoom(Long creatorId, Long guestId) {
        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Member guest = memberRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Room room = Room.createRoom(creator, guest);
        roomRepository.save(room);

        return room.getId();
    }

    @Transactional
    public Long create(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        Room room = Room.createRoom(member);
        roomRepository.save(room);

        return room.getId();
    }


    public Room findByRoomId(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomId));
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Long addGuest(String roomId, Long memberId) {
        Room room = findByRoomId(roomId);

        if (room.isFull()) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }
        if (!room.isActive()) {
            throw new IllegalStateException("비활성화된 방입니다");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        room.addGuest(member);
        return room.getId();
    }

    @Transactional
    public Long remove(String roomId) {
        Room room = findByRoomId(roomId);
        roomRepository.remove(room);
        return room.getId();
    }
}
