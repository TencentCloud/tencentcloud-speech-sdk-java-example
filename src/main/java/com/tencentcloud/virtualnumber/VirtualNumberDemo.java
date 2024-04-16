package com.tencentcloud.virtualnumber;

import com.google.gson.Gson;
import com.tencent.asrv2.*;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.core.ws.StateMachine;
import com.tencent.virtualnumber.VirtualNumberRecognizer;
import com.tencent.virtualnumber.VirtualNumberRecognizerListener;
import com.tencent.virtualnumber.VirtualNumberRecognizerRequest;
import com.tencent.virtualnumber.VirtualNumberRecognizerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * 虚拟号示例
 */
public class VirtualNumberDemo {

    static Logger logger = LoggerFactory.getLogger(VirtualNumberDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(AsrConstant.DEFAULT_RT_REQ_URL);

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        VirtualNumberRecognizerRequest request = new VirtualNumberRecognizerRequest();
        request.setVoiceFormat(12);//必填
        request.setWaitTime(60);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，遇到问题需要提供该值方便服务端排查
        // request.set("hotword_list", "腾讯云|10,语音识别|5,ASR|11"); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voice_id:{}", request.getVoiceId());
        VirtualNumberRecognizerListener listener = new VirtualNumberRecognizerListener() {
            @Override
            public void onRecognitionStart(VirtualNumberRecognizerResponse response) {//首包回调
                logger.info("{} voice_id:{},{}", "onRecognitionStart", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onRecognitionComplete(VirtualNumberRecognizerResponse response) {//识别完成回调 即final=1
                logger.info("{} voice_id:{},{}", "onRecognitionComplete", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onFail(VirtualNumberRecognizerResponse response) {//失败回调
                logger.info("{} voice_id:{},{}", "onFail", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onMessage(VirtualNumberRecognizerResponse response) {//所有消息都会回调该方法
                logger.info("{} voice_id:{},{}", "onMessage", response.getVoiceId(), new Gson().toJson(response));
            }
        };
        VirtualNumberRecognizer recognizer = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s..wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            recognizer = new VirtualNumberRecognizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            recognizer.start();
            logger.info("recognizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //如果已经识别出结果则终止发送数据
                if (recognizer.getState() == StateMachine.State.STATE_CLOSED) {
                    break;
                }
                //发送数据
                recognizer.write(speechData.get(i));
                //模拟音频间隔
                Thread.sleep(20);
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
