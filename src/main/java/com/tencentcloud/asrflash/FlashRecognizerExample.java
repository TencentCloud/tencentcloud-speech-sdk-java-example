package com.tencentcloud.asrflash;

import com.tencent.SpeechClient;
import com.tencent.asr.model.Credential;
import com.tencent.asr.model.FlashRecognitionRequest;
import com.tencent.asr.model.FlashRecognitionResponse;
import com.tencent.asr.service.FlashRecognizer;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;

/**
 * 录音识别极速版
 */
public class FlashRecognizerExample {

    /**
     * 录音识别极速版 demo
     *
     * @param args args
     */
    public static void main(String[] args) {
        boolean once = true;
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        //注意：使用前务必先填写APPID、SECRET_ID、SECRET_KEY，否则会无法运行！！！
        String APPID = "your_appid";
        String SECRET_ID = "your secretid";
        String SECRET_KEY = "your secretkey";

        Credential credential = Credential.builder().secretId(SECRET_ID).secretKey(SECRET_KEY).build();

        if (once) {
            runOnce(APPID,credential);
        } else {
            runConcurrency(APPID,credential, 10);
        }

    }

    public static void runOnce(String  APPID,Credential credential) {
        FlashRecognizer recognizer = SpeechClient.newFlashRecognizer(APPID, credential);
        byte[] data = ByteUtils.inputStream2ByteArray("test_wav/16k/16k.wav");
        //传入识别语音数据同步获取结果
        FlashRecognitionRequest recognitionRequest = FlashRecognitionRequest.initialize();
        recognitionRequest.setEngineType("16k_zh");
        recognitionRequest.setFirstChannelOnly(1);
        recognitionRequest.setVoiceFormat("wav");
        recognitionRequest.setSpeakerDiarization(0);
        recognitionRequest.setFilterDirty(0);
        recognitionRequest.setFilterModal(0);
        recognitionRequest.setFilterPunc(0);
        recognitionRequest.setConvertNumMode(1);
        recognitionRequest.setWordInfo(1);
        FlashRecognitionResponse response = recognizer.recognize(recognitionRequest, data);
        System.out.println(JsonUtil.toJson(response));
    }

    public static void runConcurrency(String  APPID,Credential credential, int threadNum) {
        for (int i = 0; i < threadNum; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnce(APPID,credential);
                }
            }).start();
        }
    }
}


