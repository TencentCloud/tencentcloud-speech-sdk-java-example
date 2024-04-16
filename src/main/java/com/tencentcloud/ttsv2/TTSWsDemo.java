package com.tencentcloud.ttsv2;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.tts.utils.Ttsutils;
import com.tencent.ttsv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;

/**
 * 实时语音合成websocket版本示例
 */
public class TTSWsDemo {

    static Logger logger = LoggerFactory.getLogger(TTSWsDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(TtsConstant.DEFAULT_TTS_REQ_URL);

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        SpeechSynthesizerRequest request = new SpeechSynthesizerRequest();
        request.setText("欢迎使用腾讯云语音合成**\n");
        request.setVoiceType(301036);
        request.setVolume(0f);
        request.setSpeed(0f);
        request.setCodec("mp3");
        request.setSampleRate(16000);
        request.setEnableSubtitle(true);
        request.setEmotionCategory("happy");
        request.setEmotionIntensity(100);
        request.setSessionId(UUID.randomUUID().toString());//sessionId，遇到问题需要提供该值方便服务端排查
        request.set("SegmentRate", 0); //sdk暂未支持参数，可通过该方法设置
        logger.debug("session_id:{}", request.getSessionId());
        SpeechSynthesizerListener listener = new SpeechSynthesizerListener() {
            byte[] audio = new byte[0];

            @Override
            public void onSynthesisStart(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisStart", response.getSessionId(), new Gson().toJson(response));
            }

            @Override
            public void onSynthesisEnd(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisEnd", response.getSessionId(), new Gson().toJson(response));
                if ("pcm".equals(request.getCodec())) {
                    Ttsutils.responsePcm2Wav(16000, audio, request.getSessionId());
                }
                if ("mp3".equals(request.getCodec())) {
                    Ttsutils.saveResponseToFile(audio, "./" + request.getSessionId() + ".mp3");
                }
            }

            @Override
            public void onAudioResult(ByteBuffer buffer) {
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                audio = ByteUtils.concat(audio, data);
            }

            @Override
            public void onTextResult(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onTextResult", response.getSessionId(), new Gson().toJson(response));
            }

            @Override
            public void onSynthesisFail(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisFail", response.getSessionId(), new Gson().toJson(response));
            }
        };
        //synthesizer不可重复使用，每次合成需要重新生成新对象
        SpeechSynthesizer synthesizer = null;
        try {
            synthesizer = new SpeechSynthesizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            synthesizer.start();
            logger.info("synthesizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            currentTimeMillis = System.currentTimeMillis();
            synthesizer.stop();
            logger.info("synthesizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (synthesizer != null) {
                synthesizer.close(); //关闭连接
            }
        }
    }
}
