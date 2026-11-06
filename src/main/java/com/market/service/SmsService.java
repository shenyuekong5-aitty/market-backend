package com.market.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeRequest;
import com.aliyuncs.dypnsapi.model.v20170525.SendSmsVerifyCodeResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class SmsService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 仅供本地答辩环境使用；生产环境必须保持 false。 */
    @Value("${sms.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${sms.mock.code:123456}")
    private String mockCode;

    @Value("${aliyun.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name:}")
    private String signName;

    @Value("${aliyun.sms.template-code:}")
    private String templateCode;

    @Value("${aliyun.sms.template-param:}")
    private String templateParam;

    public String sendVerifyCode(String phone) throws Exception {
        if (mockEnabled) {
            return mockCode;
        }
        if (accessKeyId.isBlank() || accessKeySecret.isBlank() || signName.isBlank()
                || templateCode.isBlank() || templateParam.isBlank()) {
            throw new IllegalStateException("短信服务未配置；本地演示可显式启用 sms.mock.enabled");
        }
        // 生成验证码
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

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
