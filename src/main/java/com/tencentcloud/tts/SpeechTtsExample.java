package com.tencentcloud.tts;

import com.tencent.SpeechClient;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.tts.model.*;
import com.tencent.tts.service.SpeechSynthesisListener;
import com.tencent.tts.service.SpeechSynthesizer;
import com.tencent.tts.service.SpeechSynthesisListener;
import com.tencent.tts.utils.OpusUtils;
import com.tencent.tts.utils.Ttsutils;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 语音合成 example
 */
public class SpeechTtsExample {

    private static String codec = "pcm";
    private static int sampleRate = 16000;

    private static byte[] datas = new byte[0];

    public static void main(String[] args) throws IOException {
        GlobalConfig.ifLog = true;
        //从配置文件读取密钥
        Properties props = new Properties();
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");


        //创建SpeechSynthesizerClient实例，目前是单例
        SpeechClient client = SpeechClient.newInstance(appId, secretId, secretKey);
        //初始化SpeechSynthesizerRequest，SpeechSynthesizerRequest包含请求参数
        SpeechSynthesisRequest request = SpeechSynthesisRequest.initialize();
        request.setCodec(codec);
        //request.setSampleRate(sampleRate);
        //request.setVolume(10);
        //request.setSpeed(2f);
        //request.setVoiceType(null);
        //request.setPrimaryLanguage(2);
        //request.setSampleRate(null);
        //使用客户端client创建语音合成实例
        SpeechSynthesizer speechSynthesizer = client.newSpeechSynthesizer(request, new MySpeechSynthesizerListener());
        //执行语音合成
        String ttsText = "暖国的雨，向来没有变过冰冷的坚硬的灿烂的雪花。博识的人们觉得他单调，他自己也以为不幸否耶？江南的雪，可是滋润美艳之至了.";
        speechSynthesizer.synthesis(ttsText);
    }


    public static class MySpeechSynthesizerListener extends SpeechSynthesisListener {

        private AtomicInteger sessionId = new AtomicInteger(0);

        @Override
        public void onComplete(SpeechSynthesisResponse response) {
            System.out.println("onComplete");
            if (response.getSuccess()) {
                //根据具体的业务选择逻辑处理
                if ("pcm".equals(codec)) {
                    //pcm 转 wav
                    Ttsutils.responsePcm2Wav(sampleRate, response.getAudio(), response.getSessionId());
                }
                if ("opus".equals(codec)) {
                    //opus
                    System.out.println("OPUS:" + response.getSessionId() + " length:" + response.getAudio().length);
                }
            }
            System.out.println("结束：" + response.getSuccess() + " " + response.getCode() + " " + response.getMessage() + " " + response.getEnd());
        }

        //语音合成的语音二进制数据
        @Override
        public void onMessage(byte[] data) {
            //System.out.println("onMessage:" + data.length);
            // Your own logic.
            System.out.println("onMessage length:" + data.length);
            sessionId.incrementAndGet();
        }

        @Override
        public void onFail(SpeechSynthesisResponse response) {
            System.out.println("onFail");
        }
    }

}
