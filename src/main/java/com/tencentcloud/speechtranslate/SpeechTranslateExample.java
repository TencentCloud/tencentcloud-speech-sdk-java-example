package com.tencentcloud.speechtranslate;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.speechtranslate.SpeechTranslateConstant;
import com.tencent.speechtranslate.SpeechTranslator;
import com.tencent.speechtranslate.SpeechTranslatorListener;
import com.tencent.speechtranslate.SpeechTranslatorRequest;
import com.tencent.speechtranslate.SpeechTranslatorResponse;

/**
 * 语音翻译示例
 */
public class SpeechTranslateExample {

    // SpeechClient，线程安全，应用全局创建一个即可，生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(SpeechTranslateConstant.DEFAULT_TRANSLATE_REQ_URL);
    //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
    //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
    static String appid = "";
    static String secretId = "";
    static String secretKey = "";
    static Logger logger = LoggerFactory.getLogger(SpeechTranslateExample.class);

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
        SpeechTranslatorRequest request = SpeechTranslatorRequest.init();
        request.setSource("zh"); // 源语言：中文
        request.setTarget("en"); // 目标语言：英文
        request.setTransModel("hunyuan-translation-lite"); // 翻译模型：hunyuan-translate-lite
        request.setVoiceFormat(12); // 音频格式：wav
        request.setVoiceId(UUID.randomUUID().toString()); //voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        logger.debug("voice_id:{}", request.getVoiceId());
        SpeechTranslatorListener listener = new SpeechTranslatorListener() { //tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
            @Override
            public void onTranslationStart(SpeechTranslatorResponse response) { //翻译开始回调（首包）
                logger.info("{} voice_id:{},{}", "onTranslationStart", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onSentenceBegin(SpeechTranslatorResponse response) { //一段话开始翻译
                logger.info("{} voice_id:{},{}", "onSentenceBegin", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onTranslationResultChange(SpeechTranslatorResponse response) { //翻译中间结果，source_text和target_text为非稳态结果（该段结果还可能变化）
                logger.info("{} voice_id:{},{}", "onTranslationResultChange", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onSentenceEnd(SpeechTranslatorResponse response) { //一段话翻译结束，source_text和target_text为稳态结果（该段结果不再变化）
                logger.info("{} voice_id:{},{}", "onSentenceEnd", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onTranslationComplete(SpeechTranslatorResponse response) { //翻译完成回调，即final=1
                logger.info("{} voice_id:{},{}", "onTranslationComplete", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onFail(SpeechTranslatorResponse response) { //失败回调
                logger.info("{} voice_id:{},{}", "onFail", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onMessage(SpeechTranslatorResponse response) { //所有消息都会回调该方法
                logger.info("{} voice_id:{},{}", "onMessage", response.getVoiceId(), new Gson().toJson(response));
            }
        };
        SpeechTranslator speechTranslator = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            speechTranslator = new SpeechTranslator(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            speechTranslator.start();
            logger.info("speechTranslator start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                speechTranslator.write(speechData.get(i));
                //注意：该行sleep代码用于模拟实时音频流1:1产生音频数据(每200ms产生200ms音频)，实际音频流场景建议删除该行代码，或业务根据自己的需求情况自行调整
                Thread.sleep(20);
            }
            currentTimeMillis = System.currentTimeMillis();
            speechTranslator.stop();
            //speechTranslator.stop(60000); //如果出现timeout错误，一般由于onTranslationComplete回调存在耗时操作，建议耗时操作进行异步化处理或调整stop等待时间
            logger.info("speechTranslator stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (speechTranslator != null) {
                speechTranslator.close(); //关闭连接
            }
        }
    }
}
