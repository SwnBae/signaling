package com.sign.sign.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    private String roomId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Member creator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Member guest;

    private boolean isActive;

    public static Room createRoom(Member member){
        Room room = new Room();
        room.roomId = generateRoomId();
        room.creator = member;
        room.isActive = true;

        return room;
    }

    public static Room createRoom(Member creator, Member guest){
        Room room = new Room();
        room.roomId = generateRoomId();
        room.creator = creator;
        room.guest = guest;
        room.isActive = true;

        return room;
    }

    public boolean isFull(){
        return guest != null;
    }

    public void addGuest(Member guest){
        this.guest = guest;
    }

    public void removeGuest(){
        this.guest = null;
    }

    private static String generateRoomId() {
        return RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }
}
