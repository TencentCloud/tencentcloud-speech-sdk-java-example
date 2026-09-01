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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;

/**
 * This interface service uses the websocket protocol to synthesize the requested text into audio,
 * synchronously returning the synthesized audio data and related text information, achieving the effect of "synthesizing while playing".
 * Before using this interface, you need to activate the speech synthesis service and create a key on the API key management page
 * to generate AppID, SecretID, and SecretKey, which are used to generate signatures for API calls. The signature is used for interface authentication.
 */
public class TTSWsFlowingDemo {
    // View the account APPID on the Tencent Cloud Console [Account Information](https://console.cloud.tencent.com/developer) page, and obtain SecretID and SecretKey on the [Access Management](https://console.cloud.tencent.com/cam/capi) page.
    // todo Before using this interface, you need to activate the service and replace the appId, secretId, and secretKey below with your own account information.
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static String accountArea = "0"; // Account area: "0" (default, China account), "1" International account
    static Logger logger = LoggerFactory.getLogger(TTSWsFlowingDemo.class);

    // SpeechClient only needs to be created once globally; its lifecycle can match the entire application
    static SpeechClient proxy = new SpeechClient(
            TtsConstant.ACCOUNT_AREA_INTERNATIONAL.equals(accountArea) ? TtsConstant.DEFAULT_TTS_V2_INTL_REQ_URL : TtsConstant.DEFAULT_TTS_V2_REQ_URL);

    public static void main(String[] args) throws IOException {
        process(appid, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        FlowingSpeechSynthesizerRequest request = new FlowingSpeechSynthesizerRequest();
        request.setVolume(0f);
        request.setSpeed(0f);
        request.setCodec("pcm");
        request.setSampleRate(16000);
        request.setVoiceType(101001);
        request.setEnableSubtitle(true);
        request.setEmotionCategory("neutral");
        request.setEmotionIntensity(100);
        request.setSessionId(UUID.randomUUID().toString()); // sessionId, must be globally unique (uuid is recommended). Provide this value when encountering problems to facilitate server-side troubleshooting
        request.setAccountArea(accountArea);
        //request.set("SegmentRate", 1); // parameter not yet supported by the sdk, can be set via this method
        logger.debug("session_id:{}", request.getSessionId());
        FlowingSpeechSynthesizerListener listener = new FlowingSpeechSynthesizerListener() { // tips: avoid time-consuming operations in callback methods. If there are time-consuming operations, it is recommended to handle them asynchronously, otherwise it will affect websocket request processing
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
                    try {
                        FileOutputStream out = new FileOutputStream("./test301021.mp3", false);
                        out.write(audio);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

            /**
             * Error callback; invoked when an error occurs
             * @param response response
             */
            @Override
            public void onSynthesisFail(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisFail", response.getSessionId(), new Gson().toJson(response));
            }
        };
        // synthesizer cannot be reused; a new object must be created for each synthesis
        FlowingSpeechSynthesizer synthesizer = null;
        String[] texts = {"On the Mountain of Flowers and Fruit there was ",
                "a magic stone that had absorbed the essence of ",
                "Heaven and Earth for countless ages. One day ",
                "the stone split open and a stone monkey leaped ",
                "forth, bowing to the four quarters. He became ",
                "the Handsome Monkey King, ruler of all apes. ",
                "Seeking immortality, he crossed the seas and ",
                "learned the Seventy-Two Transformations, and the ",
                "somersault cloud that carried him a hundred and ",
                "eight thousand li in a single leap. Wielding the ",
                "Golden-Hooped Rod, he made havoc in Heaven, until ",
                "the Buddha pressed him beneath the Five Elements Mountain. "
        };
        try {
            synthesizer = new FlowingSpeechSynthesizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            synthesizer.start();
            logger.info("synthesizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (String text : texts) {
                synthesizer.process(text);
                Thread.sleep(500);
            }
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
