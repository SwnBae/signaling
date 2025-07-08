package com.sign.sign.controller;

import com.sign.sign.domain.Member;
import com.sign.sign.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/regist")
    public ResponseEntity<?> register(@RequestBody Member member) {
        memberService.saveMember(member);

        return ResponseEntity.ok().build();
    }

}
