package com.tencentcloud.asrv2;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.asrv2.RealtimeRecognitionListenerV2;
import com.tencent.asrv2.RealtimeRecognitionResponseV2;
import com.tencent.asrv2.RealtimeRecognizerV2;
import com.tencent.asrv2.SentenceItem;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;

/**
 * 实时识别 V2 示例（句子模式，对齐 Go/Python v2）
 *
 * 使用 RealtimeRecognizerV2：句子模式实时识别，话者分离作为可选开关（默认关闭）。
 * 回调只有 4 个：onRecognitionStart / onRecognitionSentences / onSentenceEnd / onFail。
 */
public class RtV2Demo {

    //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
    //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static Logger logger = LoggerFactory.getLogger(RtV2Demo.class);

    public static void main(String[] args) throws InterruptedException {
        runConcurrency(1);
    }

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
        RealtimeRecognitionListenerV2 listener = new RealtimeRecognitionListenerV2() {
            @Override
            public void onRecognitionStart(RealtimeRecognitionResponseV2 response) {
                logger.info("{} voice_id:{}, speaker_context_id:{}",
                        "onRecognitionStart", response.getVoiceId(), response.getSpeakerContextId());
            }

            @Override
            public void onRecognitionSentences(RealtimeRecognitionResponseV2 response) {
                if (response.getSentences() == null || response.getSentences().getSentenceList() == null) {
                    return;
                }
                for (SentenceItem item : response.getSentences().getSentenceList()) {
                    logger.info("{} sentence_id:{}, speaker:{}, type:{}, text:{}",
                            "onRecognitionSentences", item.getSentenceId(), item.getSpeakerId(),
                            item.getSentenceType(), item.getSentence());
                }
            }

            @Override
            public void onSentenceEnd(RealtimeRecognitionResponseV2 response) {
                logger.info("{} voice_id:{}, code:{}, message:{}",
                        "onSentenceEnd", response.getVoiceId(), response.getCode(), response.getMessage());
            }

            @Override
            public void onFail(RealtimeRecognitionResponseV2 response, Exception e) {
                logger.info("{} voice_id:{}, code:{}, message:{}, error:{}",
                        "onFail", response.getVoiceId(), response.getCode(), response.getMessage(), e.getMessage());
            }
        };
        RealtimeRecognizerV2 recognizer = new RealtimeRecognizerV2(appid, credential, "16k_zh_en_speaker", listener);
        recognizer.setVoiceFormat(RealtimeRecognizerV2.AUDIO_FORMAT_WAV);
        recognizer.setNeedVad(1);
        recognizer.setSentenceStrategy(1);
        // 话者分离默认关闭；如需开启：
        //   recognizer.setSpeakerDiarization(1);
        //   recognizer.setEnableSpeakerContext(1);
        //   首次不设 speaker_context_id，start() 后从 onRecognitionStart 回调拿到，
        //   后续可设置：recognizer.setSpeakerContextId("之前拿到的id");
        // sdk 未显式定义的参数可通过扩展参数设置：
        //   recognizer.setExtraParam("hotword_list", "腾讯云|10,语音识别|5,ASR|11");
        recognizer.setVoiceId(UUID.randomUUID().toString());
        logger.debug("voice_id:{}", recognizer.getVoiceId());
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 3200);
            long currentTimeMillis = System.currentTimeMillis();
            recognizer.start();
            logger.info("recognizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                recognizer.write(speechData.get(i));
                //注意：该行sleep代码用于模拟实时音频流1:1产生音频数据，实际场景建议删除或自行调整
                Thread.sleep(100);
            }
            currentTimeMillis = System.currentTimeMillis();
            recognizer.stop();
            logger.info("recognizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            recognizer.close();
        }
    }
}
