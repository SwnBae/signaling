package com.sign.sign.domain;

import com.sign.sign.dto.request.RegistRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id @GeneratedValue
    @Column(name = "memberId")
    private long id;

    private String name;

    public static Member create(RegistRequest registRequest){
        Member member = new Member();
        member.name = registRequest.getName();
        return member;
    }
}
