package com.tencentcloud.asrv2;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.asrv2.AsrConstant;
import com.tencent.asrv2.SentenceItem;
import com.tencent.asrv2.SpeechRecognizer;
import com.tencent.asrv2.SpeechRecognizerRequest;
import com.tencent.asrv2.SpeechRecognizerResponse;
import com.tencent.asrv2.SpeechSentenceRecognizerListener;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;

/**
 * 实时识别 - 句子模式示例（result_mod=1）
 *
 * 句子模式下，服务端会将识别结果按标点切分为结构化的句子列表返回，
 * 每个句子携带独立的 sentence_id、speaker_id、时间戳等信息，客户端拿来即用。
 *
 * 与传统模式（RtDemo）的区别：
 * - 传统模式：返回不断增长的整段文本（voice_text_str），客户端需自行切句
 * - 句子模式：返回切好的句子，通过 onSentenceEnd(SentenceItem) 直接获取每句话
 */
public class RtSentenceDemo {

    // SpeechClient，线程安全，应用全局创建一个即可，生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(AsrConstant.DEFAULT_RT_REQ_URL);
    //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
    //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static Logger logger = LoggerFactory.getLogger(RtSentenceDemo.class);

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
        SpeechRecognizerRequest request = SpeechRecognizerRequest.init();
        request.setEngineModelType("16k_zh_en_vivo");
        request.setVoiceFormat(12);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        request.setNeedVad(1);
        // 开启句子模式：0=传统模式（默认），1=句子模式
        request.setResultMod(1);
        // 分句策略（仅句子模式生效）：0=单句模式（默认，有一句即返回），1=段落模式（累积多句后返回）
        request.setSentenceStrategy(1);
        // 开启话者分离（可选）：0=关闭（默认），1=开启
        request.setSpeakerDiarization(1);
        // 开启词级别时间戳（可选，需设为100）
        // request.setWordInfo(100);
        // request.set("hotword_list", "腾讯云|10,语音识别|5,ASR|11"); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voice_id:{}", request.getVoiceId());
        SpeechSentenceRecognizerListener listener = new SpeechSentenceRecognizerListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理

            @Override
            public void onSentenceBegin(SentenceItem item) {//新句子开始出现
                logger.info("{} sentence_id:{}, speaker:{}, text:{}",
                        "onSentenceBegin", item.getSentenceId(), item.getSpeakerId(), item.getSentence());
            }

            @Override
            public void onSentenceChange(SentenceItem item) {//句子内容更新（中间结果，可能变化）
                logger.info("{} sentence_id:{}, speaker:{}, text:{}",
                        "onSentenceChange", item.getSentenceId(), item.getSpeakerId(), item.getSentence());
            }

            @Override
            public void onSentenceEnd(SentenceItem item) {//句子确定（不再变化）
                logger.info("{} sentence_id:{}, speaker:{}, text:{}, time:{}~{}ms",
                        "onSentenceEnd", item.getSentenceId(), item.getSpeakerId(), item.getSentence(),
                        item.getStartTime(), item.getEndTime());
            }

            @Override
            public void onRecognitionStart(SpeechRecognizerResponse response) {//首包回调
                logger.info("{} voice_id:{}", "onRecognitionStart", response.getVoiceId());
            }

            @Override
            public void onRecognitionComplete(SpeechRecognizerResponse response) {//识别完成回调 即final=1
                logger.info("{} voice_id:{}", "onRecognitionComplete", response.getVoiceId());
            }

            @Override
            public void onFail(SpeechRecognizerResponse response) {//失败回调
                logger.info("{} voice_id:{}, code:{}, message:{}",
                        "onFail", response.getVoiceId(), response.getCode(), response.getMessage());
            }
        };
        SpeechRecognizer speechRecognizer = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 3200);
            speechRecognizer = new SpeechRecognizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            speechRecognizer.start();
            logger.info("speechRecognizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                speechRecognizer.write(speechData.get(i));
                //注意：该行sleep代码用于模拟实时音频流1:1产生音频数据(每200ms产生200ms音频)，实际音频流场景建议删除该行代码，或业务根据自己的需求情况自行调整
                Thread.sleep(100);
            }
            currentTimeMillis = System.currentTimeMillis();
            speechRecognizer.stop();
            //speechRecognizer.stop(60000); //如果出现timeout错误，一般由于onRecognitionComplete回调存在耗时操作，建议耗时操作进行异步化处理或调整stop等待时间
            logger.info("speechRecognizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (speechRecognizer != null) {
                speechRecognizer.close(); //关闭连接
            }
        }
    }
}
