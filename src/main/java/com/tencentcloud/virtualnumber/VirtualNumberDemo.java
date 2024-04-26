package com.tencentcloud.virtualnumber;

import com.google.gson.Gson;
import com.tencent.asrv2.*;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.core.ws.StateMachine;
import com.tencent.virtualnumber.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

/**
 * 虚拟号示例
 */
public class VirtualNumberDemo {

    static Logger logger = LoggerFactory.getLogger(VirtualNumberDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(VirtualNumberConstant.DEFAULT_RT_REQ_URL);

    public static void main(String[] args) {
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        String appId = "your_appid";
        String secretId = "your secretId";
        String secretKey = "your secretKey";
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        VirtualNumberRecognizerRequest request = new VirtualNumberRecognizerRequest();
        request.setVoiceFormat(12);//必填
        request.setWaitTime(60);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        // request.set("hotword_list", "腾讯云|10,语音识别|5,ASR|11"); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voice_id:{}", request.getVoiceId());
        VirtualNumberRecognizerListener listener = new VirtualNumberRecognizerListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
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
