package com.sign.sign.service;

import com.sign.sign.domain.Member;
import com.sign.sign.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long saveMember(Member member) {
        memberRepository.save(member);
        return member.getId();
    }
}
