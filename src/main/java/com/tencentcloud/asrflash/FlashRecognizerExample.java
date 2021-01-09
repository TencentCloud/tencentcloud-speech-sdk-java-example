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

    public static void main(String[] args){
        boolean once = true;
        //注意：使用前务必先填写APPID、SECRET_ID、SECRET_KEY，否则会无法运行！！！
        String APPID = "your appid";
        String SECRET_ID = "your secretid";
        String SECRET_KEY = "your secretkey";

        Credential credential = Credential.builder().secretId(SECRET_ID).secretKey(SECRET_KEY).build();
        FlashRecognizer recognizer = SpeechClient.newFlashRecognizer(APPID, credential);
        if (once) {
            runOnce(recognizer);
        } else {
            runConcurrency(recognizer, 10);
        }

    }

    public static void runOnce(FlashRecognizer recognizer) {
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

    public static void runConcurrency(final FlashRecognizer recognizer, int threadNum) {
        for (int i = 0; i < threadNum; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnce(recognizer);
                }
            }).start();
        }
    }
}


