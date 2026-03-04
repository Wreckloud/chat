package com.wreckloud.wolfchat.account.api.converter;

import com.wreckloud.wolfchat.account.api.vo.UserPublicVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;

/**
 * @Description 行者信息转换器
 * @Author Wreckloud
 * @Date 2026-03-02
 */
public final class UserConverter {
    private UserConverter() {
    }

    public static UserVO toUserVO(WfUser user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId());
        userVO.setWolfNo(user.getWolfNo());
        userVO.setEmail(user.getEmail());
        userVO.setEmailVerified(user.getEmailVerified());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setStatus(user.getStatus());
        return userVO;
    }

    public static UserPublicVO toUserPublicVO(WfUser user) {
        if (user == null) {
            return null;
        }
        UserPublicVO userVO = new UserPublicVO();
        userVO.setUserId(user.getId());
        userVO.setWolfNo(user.getWolfNo());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        userVO.setStatus(user.getStatus());
        return userVO;
    }
}
