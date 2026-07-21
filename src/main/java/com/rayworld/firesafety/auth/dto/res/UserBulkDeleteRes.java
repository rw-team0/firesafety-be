package com.rayworld.firesafety.auth.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserBulkDeleteRes {

    private List<Long> deletedUserIds;

    public static UserBulkDeleteRes from(List<Long> deletedUserIds) {
        return new UserBulkDeleteRes(deletedUserIds);
    }
}
