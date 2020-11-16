
package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.model.*;
import com.tencent.asr.service.SpeechRecognitionListener;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * websocket 语音实时识别参考案例
 */
public class SpeechRecognitionWebsocketExample {

    public static void main(String[] args) throws InterruptedException, IOException {
        //GlobalConfig.ifLog = true; //是否开启sdk日志打印

        //从配置文件读取密钥（可自行修改）
        Properties props = new Properties();
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");

        //对流进行处理，语音识别
        //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
        FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
        List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 6400);


        //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
        SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
        request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
        request.setVoiceFormat(1);  //指定音频格式


        SpeechClient client = SpeechClient.newInstance(appId, secretId, secretKey);
        SpeechRecognizer speechWsRecognizer = client.newSpeechRecognizer(request, new MySpeechRecognitionListener());
        //开始识别 调用start方法
        speechWsRecognizer.start();
        for (int i = 0; i < speechData.size(); i++) {
            //模拟音频间隔
            Thread.sleep(200);
            //发送数据
            speechWsRecognizer.write(speechData.get(i));
        }
        //结束识别调用stop方法
        speechWsRecognizer.stop();


        fileInputStream.close();
    }


    /**
     * 回调方法
     * 方法回调顺序为  onRecognitionStart -> onSentenceBegin->onRecognitionResultChange->onSentenceEnd->onRecognitionComplete
     */
    public static class MySpeechRecognitionListener extends SpeechRecognitionListener {

        @Override
        public void onRecognitionResultChange(SpeechRecognitionResponse response) {
            System.out.println("识别结果:" + JsonUtil.toJson(response));
        }

        @Override
        public void onRecognitionStart(SpeechRecognitionResponse response) {
            System.out.println("开始识别:" + JsonUtil.toJson(response));
        }

        @Override
        public void onSentenceBegin(SpeechRecognitionResponse response) {
            System.out.println("一句话开始:" + JsonUtil.toJson(response));
        }

        @Override
        public void onSentenceEnd(SpeechRecognitionResponse response) {
            System.out.println("一句话结束:" + JsonUtil.toJson(response));
        }

        @Override
        public void onRecognitionComplete(SpeechRecognitionResponse response) {
            System.out.println("识别结束:" + JsonUtil.toJson(response));
        }

        @Override
        public void onFail(SpeechRecognitionResponse response) {
            System.out.println("错误:" + JsonUtil.toJson(response));
        }
    }
}

