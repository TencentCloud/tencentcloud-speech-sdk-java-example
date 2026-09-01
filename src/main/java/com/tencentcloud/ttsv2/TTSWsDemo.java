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
 * Real-time speech synthesis WebSocket version example
 */
public class TTSWsDemo {
    // View the account APPID on the Tencent Cloud Console [Account Information](https://console.cloud.tencent.com/developer) page, and obtain SecretID and SecretKey on the [Access Management](https://console.cloud.tencent.com/cam/capi) page.
    // todo Before using this interface, you need to activate the service and replace the appId, secretId, and secretKey below with your own account information.
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static String accountArea = "0"; // Account area: "0" (default, China account), "1" International account
    static Logger logger = LoggerFactory.getLogger(TTSWsDemo.class);

    // SpeechClient only needs to be created once globally; its lifecycle can match the entire application
    static SpeechClient proxy = new SpeechClient(
            TtsConstant.ACCOUNT_AREA_INTERNATIONAL.equals(accountArea) ? TtsConstant.DEFAULT_TTS_INTL_REQ_URL : TtsConstant.DEFAULT_TTS_REQ_URL);

    public static void main(String[] args) throws InterruptedException {
        runConcurrency(1);
        proxy.shutdown();
    }

    /**
     * Concurrency
     *
     * @param client SpeechClient
     * @param threadNum thread count
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
        request.setText("Welcome to Tencent Cloud Speech Synthesis**\n");
        request.setVoiceType(101001);
        request.setVolume(0f);
        request.setSpeed(0f);
        request.setCodec("mp3");
        request.setSampleRate(16000);
        request.setEnableSubtitle(true);
        request.setEmotionCategory("happy");
        request.setEmotionIntensity(100);
        request.setSessionId(UUID.randomUUID().toString()); // sessionId, must be globally unique (uuid is recommended). Provide this value when encountering problems to facilitate server-side troubleshooting
        request.setAccountArea(accountArea);
        request.set("SegmentRate", 0); // parameter not yet supported by the sdk, can be set via this method
        logger.debug("session_id:{}", request.getSessionId());
        SpeechSynthesizerListener listener = new SpeechSynthesizerListener() { // tips: avoid time-consuming operations in callback methods. If there are time-consuming operations, it is recommended to handle them asynchronously, otherwise it will affect websocket request processing
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
        // synthesizer cannot be reused; a new object must be created for each synthesis
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
                synthesizer.close(); // close connection
            }
        }
    }
}
