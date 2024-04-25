package com.tencentcloud.asrv2;

import com.google.gson.Gson;
import com.tencent.asrv2.*;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

/**
 * 实时识别示例
 */
public class RtDemo {

    static Logger logger = LoggerFactory.getLogger(RtDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(AsrConstant.DEFAULT_RT_REQ_URL);

    public static void main(String[] args) {
        String appId = "your appid";
        String secretId = "your secretId";
        String secretKey = "your secretKey";
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        SpeechRecognizerRequest request = SpeechRecognizerRequest.init();
        request.setEngineModelType("8k_zh");
        request.setVoiceFormat(12);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，遇到问题需要提供该值方便服务端排查
        // request.set("hotword_list", "腾讯云|10,语音识别|5,ASR|11"); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voice_id:{}", request.getVoiceId());
        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            @Override
            public void onRecognitionStart(SpeechRecognizerResponse response) {//首包回调
                logger.info("{} voice_id:{},{}", "onRecognitionStart", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onSentenceBegin(SpeechRecognizerResponse response) {//一段话开始识别 slice_type=0
                logger.info("{} voice_id:{},{}", "onSentenceBegin", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onRecognitionResultChange(SpeechRecognizerResponse response) {//一段话识别中，slice_type=1,voice_text_str 为非稳态结果(该段识别结果还可能变化)
                logger.info(" {} voice_id:{},{}", "onRecognitionResultChange", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onSentenceEnd(SpeechRecognizerResponse response) {//一段话识别结束，slice_type=2,voice_text_str 为稳态结果(该段识别结果不再变化)
                logger.info("{} voice_id:{},{}", "onSentenceEnd", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onRecognitionComplete(SpeechRecognizerResponse response) {//识别完成回调 即final=1
                logger.info("{} voice_id:{},{}", "onRecognitionComplete", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onFail(SpeechRecognizerResponse response) {//失败回调
                logger.info("{} voice_id:{},{}", "onFail", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onMessage(SpeechRecognizerResponse response) {//所有消息都会回调该方法
                logger.info("{} voice_id:{},{}", "onMessage", response.getVoiceId(), new Gson().toJson(response));
            }
        };
        SpeechRecognizer speechRecognizer = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/8k/8k_19s.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            speechRecognizer = new SpeechRecognizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            speechRecognizer.start();
            logger.info("speechRecognizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                speechRecognizer.write(speechData.get(i));
                //模拟音频间隔
                Thread.sleep(20);
            }
            currentTimeMillis = System.currentTimeMillis();
            speechRecognizer.stop();
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
