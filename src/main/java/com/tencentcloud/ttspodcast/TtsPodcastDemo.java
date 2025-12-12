package com.tencentcloud.ttspodcast;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.tts.utils.Ttsutils;
import com.tencent.ttspodcast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TtsPodcastDemo {
    static Logger logger = LoggerFactory.getLogger(TtsPodcastDemo.class);

    static SpeechClient proxy = new SpeechClient(TtsPodcastConstant.DEFAULT_PODCAST_REQ_URL);

    public static void main(String[] args) {
        String appId = "";
        String secretId = "";
        String secretKey = "";

        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        TtsPodcastRequest request = new TtsPodcastRequest();
        request.setCodec("pcm");
        request.setSampleRate(24000);

        TtsPodcastListener listener = new TtsPodcastListener() {
            private byte[] audio = new byte[0];

            @Override
            public void onSynthesisStart(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisStart", response.getSessionId(), new Gson().toJson(response));
            }

            @Override
            public void onSynthesisEnd(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisEnd", response.getSessionId(), new Gson().toJson(response));
                String codec = request.getCodec();
                if ("pcm".equalsIgnoreCase(codec)) {
                    Ttsutils.responsePcm2Wav(request.getSampleRate(), audio, response.getSessionId());
                } else if ("mp3".equalsIgnoreCase(codec)) {
                    try (FileOutputStream out = new FileOutputStream("./" + response.getSessionId() + ".mp3", false)) {
                        out.write(audio);
                    } catch (IOException e) {
                        logger.error("fail to write mp3 file", e);
                    }
                } else {
                    logger.info("codec {}: sdk NOT implemented, please save the file yourself", codec);
                }
            }

            @Override
            public void onAudioResult(ByteBuffer buffer) {
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                audio = ByteUtils.concat(audio, data);
            }

            @Override
            public void onTextResult(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onTextResult", response.getSessionId(), new Gson().toJson(response));
                if (response.getResult() != null && response.getResult().getScripts() != null) {
                    for (TtsPodcastScripts script : response.getResult().getScripts()) {
                        logger.info("script[{}]: text={} speaker={} begin_time={} end_time={}",
                                script.getIndex(), script.getText(), script.getSpeaker(),
                                script.getBeginTime(), script.getEndTime());
                    }
                }
            }

            @Override
            public void onSynthesisFail(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisFail", response.getSessionId(), new Gson().toJson(response));
            }
        };

        TtsPodcastSynthesizer synthesizer = null;
        try {
            synthesizer = new TtsPodcastSynthesizer(proxy, credential, request, listener);

            // 添加播客资料，这里以文件 URL 示例
             String fileUrl = "https://justin-dev-1300466766.cos.ap-shanghai.myqcloud.com/public_link/news.pdf";
             synthesizer.addFile(fileUrl, "pdf");

            long currentTimeMillis = System.currentTimeMillis();
            synthesizer.start();
            logger.info("synthesizer start latency : {} ms", System.currentTimeMillis() - currentTimeMillis);

            currentTimeMillis = System.currentTimeMillis();
            synthesizer.process();
            logger.info("synthesizer process latency : {} ms", System.currentTimeMillis() - currentTimeMillis);
        } catch (Exception e) {
            logger.error("synthesizer error", e);
        } finally {
            if (synthesizer != null) {
                synthesizer.close("final");
            }
        }
    }
}
