# 简介
欢迎使用腾讯云语音SDK，腾讯云语音SDK为开发者提供了访问腾讯云语音识别、语音合成等语音服务的配套开发工具，简化腾讯云语音服务的接入流程。
# 依赖环境
1. 依赖环境: JDK 1.8版本及以上
2. 使用相关产品前需要在腾讯云控制台已开通相关语音产品。
3. 在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。


# 获取安装
安装 Java SDK 前,先获取安全凭证。在第一次使用SDK之前，用户首先需要在腾讯云控制台上申请安全凭证，安全凭证包括 SecretID 和 SecretKey，SecretID 是用于标识 API 调用者的身份，SecretKey 是用于加密签名字符串和服务器端验证签名字符串的密钥 SecretKey 必须严格保管，避免泄露。


## 通过 Maven 安装
从maven服务器下载最新版本SDK
```xml
<!-- https://mvnrepository.com/artifact/com.tencentcloudapi/tencentcloud-speech-sdk-java -->
<dependency>
    <groupId>com.tencentcloudapi</groupId>
    <artifactId>tencentcloud-speech-sdk-java</artifactId>
    <version>最新版本</version>
</dependency>
```

## 常见问题：

1.ASR、TTS 、SOE配置代理

本示例为soe示例，其他业务类似。
```
nginx配置正向代理参考（注：仅供参考，具体根据业务自行调整）
server {
listen       80;
location / {
       proxy_pass https://soe.cloud.tencent.com; #替换为使用业务域名
       proxy_redirect off;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header Upgrade $http_upgrade;
       }
}
```

```
static {
 OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL = "ws://127.0.0.1:80/soe/api/"; //ip port替换为自己的代理地址 /soe/api/ 替换为使用业务后缀
}
static SpeechClient proxy = new SpeechClient(OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL);
```

2.出现timeout after %d ms waiting for stop错误信息

```
synthesizer.stop();// 由于stop方法执行超时出现该错误，可通过synthesizer.stop(60000); 调整stop方法超时时间来避免
```

3.调整连接失败重试次数
```
static {
SpeechClient.connectMaxTryTimes=3; //失败重试次数
}
```