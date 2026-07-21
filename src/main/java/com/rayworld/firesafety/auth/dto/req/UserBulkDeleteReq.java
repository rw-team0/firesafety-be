package com.rayworld.firesafety.auth.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserBulkDeleteReq {

    private List<Long> userIds;
}
