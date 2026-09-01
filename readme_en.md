# Introduction
Welcome to Tencent Cloud Speech SDK. It provides developers with supporting tools to access Tencent Cloud speech services such as Automatic Speech Recognition (ASR) and Text-To-Speech (TTS), simplifying the integration process with Tencent Cloud speech services.

# Environment Requirements
1. JDK 1.8 or above.
2. The relevant speech products must be activated in the Tencent Cloud Console before using related services.
3. View your account APPID on the [Account Information](https://console.cloud.tencent.com/developer) page of the Tencent Cloud Console, and obtain your SecretID and SecretKey on the [Access Management](https://console.cloud.tencent.com/cam/capi) page.

# Installation
Before installing the Java SDK, obtain your security credentials first. Before using the SDK for the first time, you need to apply for security credentials in the Tencent Cloud Console. The security credentials include SecretID and SecretKey. SecretID is used to identify the identity of the API caller, and SecretKey is used to encrypt the signature string and verify the signature string on the server side. SecretKey must be kept strictly confidential to avoid leakage.

## Install via Maven
Download the latest version of the SDK from the Maven repository.
```xml
<!-- https://mvnrepository.com/artifact/com.tencentcloudapi/tencentcloud-speech-sdk-java -->
<dependency>
    <groupId>com.tencentcloudapi</groupId>
    <artifactId>tencentcloud-speech-sdk-java</artifactId>
    <version>latest version</version>
</dependency>
```

## FAQ

1. Configure proxy for ASR, TTS, and SOE

   This example demonstrates the SOE scenario; other services are similar.

   ```
   nginx forward proxy configuration reference (for reference only; adjust according to your business needs)
   server {
   listen       80;
   location / {
          proxy_pass https://soe.cloud.tencent.com; # replace with your business domain
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
    OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL = "ws://127.0.0.1:80/soe/api/"; // replace ip and port with your proxy address; replace /soe/api/ with your business suffix
   }
   static SpeechClient proxy = new SpeechClient(OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL);
   ```

2. The error "timeout after %d ms waiting for stop" appears

   ```
   synthesizer.stop();// This error occurs because the stop method timed out. You can avoid it by adjusting the stop timeout, e.g. synthesizer.stop(60000);
   ```

3. Adjust the connection failure retry count

   ```
   static {
   SpeechClient.connectMaxTryTimes=3; // connection failure retry count
   }
   ```
