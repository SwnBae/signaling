package com.sign.sign.repository;

import com.sign.sign.domain.Room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

    @PersistenceContext
    private EntityManager em;

    public Long save(Room room) {
        em.persist(room);
        return room.getId();
    }

    public Optional<Room> findById(Long id) {
        return Optional.ofNullable(em.find(Room.class, id));
    }

    public Optional<Room> findByRoomId(String roomId) {
        return Optional.ofNullable(em.find(Room.class, roomId));
    }

    public void remove(Room room) {
        em.remove(room);
    }
}
