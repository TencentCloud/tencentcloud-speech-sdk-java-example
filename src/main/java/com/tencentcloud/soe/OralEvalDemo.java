package com.tencentcloud.soe;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.soe.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

/**
 * 智能口语评测示例
 */
public class OralEvalDemo {

    static Logger logger = LoggerFactory.getLogger(OralEvalDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL);

    public static void main(String[] args) {
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        String appId = "your_appid";
        String secretId = "your secretId";
        String secretKey = "your secretKey";
        // 只有临时秘钥鉴权需要
        String token = "";
        process(appId, secretId, secretKey, token);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey, String token) {
        Credential credential = new Credential(appId, secretId, secretKey);
        credential.setToken(token);
        OralEvaluationRequest request = new OralEvaluationRequest();
        request.setVoiceFormat(1);
        request.setRefText("床前明月光");
        request.setServerEngineType("16k_zh");
        request.setScoreCoeff(1.1);
        request.setEvalMode(1);
        request.setKeyword("明月");
        request.setSentenceInfoEnabled(1);
        request.setRecMode(1);
        request.setVoiceId(UUID.randomUUID().toString());//voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        // request.set("voice_id", UUID.randomUUID().toString()); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voice_id:{}", request.getVoiceId());
        OralEvaluationListener listener = new OralEvaluationListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
            @Override
            public void OnIntermediateResults(OralEvaluationResponse response) {//评测中回调
                logger.info("{} voice_id:{},{}", "OnIntermediateResults", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onRecognitionStart(OralEvaluationResponse response) {//首包回调
                logger.info("{} voice_id:{},{}", "onRecognitionStart", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onRecognitionComplete(OralEvaluationResponse response) {//识别完成回调 即final=1
                logger.info("{} voice_id:{},{}", "onRecognitionComplete", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onFail(OralEvaluationResponse response) {//失败回调
                logger.info("{} voice_id:{},{}", "onFail", response.getVoiceId(), new Gson().toJson(response));
            }

            @Override
            public void onMessage(OralEvaluationResponse response) {//所有消息都会回调该方法
                logger.info("{} voice_id:{},{}", "onMessage", response.getVoiceId(), new Gson().toJson(response));
            }
        };
        OralEvaluator oralEvaluator = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 6400);
            oralEvaluator = new OralEvaluator(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            oralEvaluator.start();
            logger.info("OralEvalDemo start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                oralEvaluator.write(speechData.get(i));
                //模拟音频间隔
                Thread.sleep(200);
            }
            currentTimeMillis = System.currentTimeMillis();
            oralEvaluator.stop();
            logger.info("OralEvalDemo stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (oralEvaluator != null) {
                oralEvaluator.close(); //关闭连接
            }
        }
    }
}
