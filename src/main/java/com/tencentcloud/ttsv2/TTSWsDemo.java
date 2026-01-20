package com.tencentcloud.ttsv2;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.tts.utils.Ttsutils;
import com.tencent.ttsv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * 实时语音合成websocket版本示例
 */
public class TTSWsDemo {

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(TtsConstant.DEFAULT_TTS_REQ_URL);
    //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
    //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static Logger logger = LoggerFactory.getLogger(TTSWsDemo.class);

    public static void main(String[] args) throws InterruptedException {
        runConcurrency(1);
        proxy.shutdown();
    }

    /**
     * 并发
     *
     * @param client SpeechClient
     * @param threadNum 线程数
     * @throws InterruptedException InterruptedException
     */
    public static void runConcurrency(int threadNum) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            Thread.sleep(50);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnce();
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
    }

    public static void runOnce() {
        Credential credential = new Credential(appid, secretId, secretKey);
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
        request.setSessionId(UUID.randomUUID().toString());//sessionId，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        request.set("SegmentRate", 0); //sdk暂未支持参数，可通过该方法设置
        logger.debug("session_id:{}", request.getSessionId());
        SpeechSynthesizerListener listener = new SpeechSynthesizerListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
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
