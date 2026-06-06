package com.market.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String avatar;
    private Integer gender;         // 0-女, 1-男, 2-保密
    private String newPhone;        // 可选，修改手机号时填写
    private String phoneCode;       // 新手机号验证码
    private String confirmPassword; // 修改手机号时的密码验证
}