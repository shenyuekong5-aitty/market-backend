package com.market.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeRequest;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class SmsService {

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code}")
    private String templateCode;

    @Value("${aliyun.sms.template-param}")
    private String templateParam;

    public String sendVerifyCode(String phone) throws Exception {
        // 生成验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 创建认证客户端
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Dypnsapi", "dypnsapi.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);

        // 构建请求
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest();
        request.setPhoneNumber(phone);
        request.setSignName(signName);
        request.setTemplateCode(templateCode);
        // 替换模板变量
        String finalParam = templateParam.replace("{code}", code);
        request.setTemplateParam(finalParam);

        // 发送
        SendSmsVerifyCodeResponse response = client.getAcsResponse(request);
        if (!"OK".equals(response.getCode())) {
            throw new RuntimeException("短信发送失败：" + response.getMessage());
        }
        return code;
    }
}