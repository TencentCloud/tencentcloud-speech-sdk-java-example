package com.tencentcloud.tts;

import com.tencent.SpeechClient;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;
import com.tencent.tts.model.SpeechWsSynthesisRequest;
import com.tencent.tts.model.SpeechWsSynthesisResponse;
import com.tencent.tts.model.SpeechWsSynthesisServerConfig;
import com.tencent.tts.service.SpeechWsSynthesisListener;
import com.tencent.tts.service.SpeechWsSynthesizer;
import com.tencent.tts.utils.Ttsutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 实时语音合成websocket
 */
public class SpeechWsExample {

    static class WsListener extends SpeechWsSynthesisListener {

        byte[] audio;
        int index = 0;
        String codec;

        public WsListener(String id) {
            super(id);
            audio = new byte[0];
        }

        public WsListener(String id, String codec) {
            super(id);
            audio = new byte[0];
            this.codec = codec;
        }

        public void onSynthesisStart(SpeechWsSynthesisResponse response) {
            String message = "onSynthesisStart: ".concat(this.Id).concat(" ").concat(new Date().toString());
            System.out.println(message);
        }

        public void onSynthesisEnd(SpeechWsSynthesisResponse response) {
            String message = "onSynthesisEnd: ".concat(this.Id).concat(" ").concat(new Date().toString());
            System.out.println(message);
            if ("pcm".equals(codec)) {
                Ttsutils.responsePcm2Wav(16000, audio, this.Id);
            }
            if ("mp3".equals(codec)) {
                Ttsutils.saveResponseToFile(audio, "./" + this.Id + ".mp3");
            }
        }

        public void onAudioResult(byte[] data) {
            String message = "onAudioResult: ".concat(this.Id).concat(" " + index + " " + data.length + " ").concat(new Date().toString());
            audio = ByteUtils.concat(audio, data);
            System.out.println(message);
            index++;
        }

        public void onTextResult(SpeechWsSynthesisResponse response) {
            String message = "onTextResult: ".concat(this.Id).concat(" ").concat(new Date().toString());
            System.out.println(message + JsonUtil.toJson(response));
        }

        public void onSynthesisFail(SpeechWsSynthesisResponse response) {
            String message = "onSynthesisFail: ".concat(this.Id).concat(" ").concat(new Date().toString());
            System.out.println(message + response.getSessionId() + response.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        GlobalConfig.ifLog = true;
        //从配置文件读取密钥
        Properties props = new Properties();
        props.load(new FileInputStream("../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");

        // 初始化request
        SpeechWsSynthesisRequest request = new SpeechWsSynthesisRequest();
        request.setAppId(Integer.valueOf(appId));
        request.setSecretId(secretId);
        request.setSecretKey(secretKey);
        request.setSessionId(UUID.randomUUID().toString());
        request.setVoiceType(301035);
        request.setVolume(0f);
        request.setSpeed(0f);
        request.setCodec("mp3");
        request.setSampleRate(16000L);
        request.setEnableSubtitle(true);
        request.setEmotionCategory("sad");
        request.setEmotionIntensity(100);
        request.setText("欢迎使用腾讯云实时语音合成,欢迎使用腾讯云实时语音,欢迎使用腾讯云实时语音,欢迎使用腾讯云实时语音");

        //SpeechWsSynthesisServerConfig 全局唯一 保持单例
        SpeechWsSynthesisServerConfig c = SpeechWsSynthesisServerConfig.getInstance();
        //c.setConnectTime(5000); 如果网络较差可调大连接超时时间

        //用于标识回调 自定义随机字符串
        String listenerId = UUID.randomUUID().toString();
        WsListener listener = new WsListener(listenerId, request.getCodec());
        SpeechWsSynthesizer synthesizer = SpeechClient.newSpeechWsSynthesizer(c, request, listener);
        try {
            synthesizer.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            synthesizer.stop();
        }
    }
}
