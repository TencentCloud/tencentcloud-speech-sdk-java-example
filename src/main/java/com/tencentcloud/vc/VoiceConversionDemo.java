package com.tencentcloud.vc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.tts.utils.Ttsutils;
import com.tencent.vc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

/**
 * 声音变换（websocket）
 * https://cloud.tencent.com/document/product/1664/85973
 */
public class VoiceConversionDemo {

    static Logger logger = LoggerFactory.getLogger(VoiceConversionDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient("wss://tts.cloud.tencent.com/vc_stream/");

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
        VoiceConversionRequest request = new VoiceConversionRequest();
        request.setVoiceType(301011);
        request.setVolume(0f);
        request.setCodec("pcm");
        request.setSampleRate(16000);
        request.setVoiceId(UUID.randomUUID().toString());//VoiceId，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        //request.set("", 0); //sdk暂未支持参数，可通过该方法设置
        logger.debug("voiceId:{}", request.getVoiceId());
        VoiceConversionListener listener = new VoiceConversionListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
            Gson gson = new GsonBuilder().create();

            @Override
            public void OnVoiceConversionStart(VoiceConversionResponse response) {
                logger.info("{} session_id:{},{}", "OnVoiceConversionStart", response.getVoiceId(), response.getMessage());
            }

            @Override
            public void OnVoiceConversionResultChange(VoiceConversionResponse response) {
                logger.info("{} session_id:{},{}", "OnVoiceConversionResultChange", response.getVoiceId(), response.getMessage());
                if (response.getAudio() != null) {
                    audio = ByteUtils.concat(audio, response.getAudio());
                }
            }

            @Override
            public void OnVoiceConversionComplete(VoiceConversionResponse response) {
                logger.info("{} session_id:{},{}", "OnVoiceConversionComplete", response.getVoiceId(), response.getMessage());
                Ttsutils.responsePcm2Wav(request.getSampleRate(), audio, request.getVoiceId());
            }

            @Override
            public void OnFail(VoiceConversionResponse response) {
                logger.info("{} session_id:{},{}", "OnFail", response.getVoiceId(), response.getMessage());
            }

            byte[] audio = new byte[0];

        };
        //voiceConverter不可重复使用，每次合成需要重新生成新对象
        VoiceConverter voiceConverter = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s..wav"));
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            voiceConverter = new VoiceConverter(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            voiceConverter.start();
            logger.info("voiceConverter start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (int i = 0; i < speechData.size(); i++) {
                //发送数据
                logger.info("voiceConverter send data : " + speechData.get(i).length);
                voiceConverter.write(speechData.get(i));
                //模拟音频间隔
                Thread.sleep(20);
            }
            currentTimeMillis = System.currentTimeMillis();
            voiceConverter.stop();
            logger.info("voiceConverter stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (voiceConverter != null) {
                voiceConverter.close(); //关闭连接
            }
        }
    }
}
