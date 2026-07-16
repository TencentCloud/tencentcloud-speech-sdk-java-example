package com.tencentcloud.asrspeaker;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.asrspeaker.SpeakerConstant;
import com.tencent.asrspeaker.SpeakerRecognitionListener;
import com.tencent.asrspeaker.SpeakerRecognitionResponse;
import com.tencent.asrspeaker.SpeakerRecognizer;
import com.tencent.asrspeaker.SpeakerRecognizerRequest;
import com.tencent.asrspeaker.SpeakerSentenceItem;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;

/**
 * 实时识别 - 话者分离（句子模式）示例
 *
 * 基于句子模式（result_mod=1），开启话者分离和断点续传：
 * - 服务端按句子粒度返回识别结果，每个句子携带 speaker_id 标识说话人
 * - 首次连接时在 onRecognitionStart 中获取 speakerContextId，后续会话回传可保持话者编号一致
 */
public class RtSpeakerDemo {

    // SpeechClient，线程安全，应用全局创建一个即可，生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(SpeakerConstant.DEFAULT_RT_REQ_URL);
    //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
    //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static Logger logger = LoggerFactory.getLogger(RtSpeakerDemo.class);

    public static void main(String[] args) throws InterruptedException {
        runConcurrency(1);
        proxy.shutdown();
    }

    /**
     * 并发
     *
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

    /**
     * 单独执行
     */
    public static void runOnce() {
        Credential credential = new Credential(appid, secretId, secretKey);
        SpeakerRecognizerRequest request = SpeakerRecognizerRequest.init();
        request.setEngineModelType("16k_zh_en_speaker");
        request.setVoiceFormat(SpeakerConstant.AUDIO_FORMAT_WAV);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        request.setNeedVad(1);
        request.setVadSilenceTime(1000);
        request.setSentenceStrategy(1);
        // 开启话者分离
        request.setSpeakerDiarization(1);
        // 开启断点续传（可选）：首次不设 speakerContextId，从 onRecognitionStart 回调中获取
        request.setEnableSpeakerContext(1);
        // 后续会话可设置：request.setSpeakerContextId("之前获取的 id");
        logger.debug("voice_id:{}", request.getVoiceId());

        SpeakerRecognitionListener listener = new SpeakerRecognitionListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理

            @Override
            public void onRecognitionStart(SpeakerRecognitionResponse response) {//首包回调
                logger.info("{} voice_id:{}, speaker_context_id:{}",
                        "onRecognitionStart", response.getVoiceId(), response.getSpeakerContextId());
            }

            @Override
            public void onRecognitionSentences(SpeakerRecognitionResponse response) {//句子列表回调
                if (response.getSentences() == null) {
                    return;
                }
                for (SpeakerSentenceItem item : response.getSentences().getSentenceList()) {
                    if (item.getSentenceType() == 1) {
                        logger.info("{} sentence_id:{}, speaker:{}, text:{}, time:{}~{}ms",
                                "onSentenceEnd", item.getSentenceId(), item.getSpeakerId(), item.getSentence(),
                                item.getStartTime(), item.getEndTime());
                    } else {
                        logger.info("{} sentence_id:{}, speaker:{}, text:{}",
                                "onSentenceChange", item.getSentenceId(), item.getSpeakerId(), item.getSentence());
                    }
                }
            }

            @Override
            public void onSentenceEnd(SpeakerRecognitionResponse response) {//识别完成回调 即final=1
                logger.info("{} voice_id:{}", "onSentenceEnd", response.getVoiceId());
            }

            @Override
            public void onFail(SpeakerRecognitionResponse response, Exception error) {//失败回调
                logger.info("{} voice_id:{}, code:{}, message:{}",
                        "onFail", response.getVoiceId(), response.getCode(), response.getMessage());
            }
        };

        SpeakerRecognizer recognizer = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 3200);
            recognizer = new SpeakerRecognizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            recognizer.start();
            logger.info("recognizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                recognizer.write(speechData.get(i));
                //注意：该行sleep代码用于模拟实时音频流1:1产生音频数据(每200ms产生200ms音频)，实际音频流场景建议删除该行代码，或业务根据自己的需求情况自行调整
                Thread.sleep(100);
            }
            currentTimeMillis = System.currentTimeMillis();
            recognizer.stop();
            logger.info("recognizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (recognizer != null) {
                recognizer.close(); //关闭连接
            }
        }
    }
}