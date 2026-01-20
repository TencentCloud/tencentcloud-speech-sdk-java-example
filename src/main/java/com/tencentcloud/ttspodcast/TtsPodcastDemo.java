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

    String appid = "";
    String secretId = "";
    String secretKey = "";
    String codec = "pcm";
    Integer sampleRate = 24000;
    Integer speakerNumber = 0;
    String speaker1Voice = "";
    String speaker2Voice = "";
    String sessionId = "";
    String contextId = "";
    Boolean enableWebSearch = false;

    public static void main(String[] args) {
        TtsPodcastDemo demo = new TtsPodcastDemo();
        demo.process();
        proxy.shutdown();
    }

    public void process() {
        Credential credential = new Credential(appid, secretId, secretKey);
        TtsPodcastRequest request = new TtsPodcastRequest();
        request.setCodec(codec);
        request.setSampleRate(sampleRate);
        request.setSpeakerNumber(speakerNumber);
        request.setSpeaker1Voice(speaker1Voice);
        request.setSpeaker2Voice(speaker2Voice);
        request.setSessionId(sessionId);
        request.setContextId(contextId);
        request.setEnableWebSearch(enableWebSearch);

        TtsPodcastListener listener = new TtsPodcastListener() {
            private byte[] audio = new byte[0];

            @Override
            public void onSynthesisStart(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisStart", response.getSessionId(),
                        new Gson().toJson(response));
            }

            @Override
            public void onSynthesisEnd(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisEnd", response.getSessionId(),
                        new Gson().toJson(response));
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
                logger.info("{} session_id:{},{}", "onTextResult", response.getSessionId(),
                        new Gson().toJson(response));
                if (response.getResult() != null) {
                    TtsPodcastResult result = response.getResult();
                    if (result.getScripts() != null) {
                        for (TtsPodcastScripts script : response.getResult().getScripts()) {
                            logger.info("script[{}]: text={} speaker={} begin_time={} end_time={}",
                                    script.getIndex(), script.getText(), script.getSpeaker(),
                                    script.getBeginTime(), script.getEndTime()); // 脚本时间戳
                        }
                    }
                    // 上下文 ID（用于交互模式）
                    if (result.getContextId() != null) {
                        logger.info("context_id:{}", result.getContextId());
                    }
                    // token 消耗量
                    if (result.getUsageTokens() != null) {
                        int inTokens = result.getUsageTokensInput(),
                                outTokens = result.getUsageTokensOutput();
                        logger.info("token usage, in={}, out={}", inTokens, outTokens);
                    }
                }
            }

            @Override
            public void onSynthesisFail(TtsPodcastResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisFail", response.getSessionId(),
                        new Gson().toJson(response));
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

            // 启动定时器，30秒后取消合成
            // final TtsPodcastSynthesizer finalSynthesizer = synthesizer;
            // new Thread(() -> {
            //     try {
            //         Thread.sleep(30000);
            //         logger.info("cancel synthesize...");
            //         finalSynthesizer.cancel();
            //         logger.info("cancelled successfully");
            //     } catch (InterruptedException e) {
            //         logger.error("cancel failed", e);
            //     }
            // }).start();

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
