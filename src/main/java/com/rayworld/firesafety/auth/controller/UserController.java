package com.rayworld.firesafety.auth.controller;

import com.rayworld.firesafety.auth.dto.req.UserCreateReq;
import com.rayworld.firesafety.auth.dto.res.UserCreateRes;
import com.rayworld.firesafety.auth.dto.res.UserListRes;
import com.rayworld.firesafety.auth.service.UserService;
import com.rayworld.firesafety.common.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // SUPER_ADMIN 전용 계정 목록. 삭제된 사용자는 운영 목록에서 제외한다.
    @GetMapping
    public ResultResponse<List<UserListRes>> getUsers() {
        List<UserListRes> users = userService.getUsers();
        return ResultResponse.success(String.format("%d rows", users.size()), users);
    }

    // 관리자 화면에서 하위 계정을 생성한다. 공개 회원가입 API가 아니다.
    @PostMapping
    public ResultResponse<UserCreateRes> createUser(@RequestBody UserCreateReq req) {
        UserCreateRes user = userService.createUser(req);
        return ResultResponse.success("계정 등록 성공", user);
    }
}
