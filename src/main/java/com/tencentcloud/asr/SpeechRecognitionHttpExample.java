package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.*;
import com.tencent.asr.service.SpeechRecognitionListener;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * http 语音实时识别参考案例
 */
public class SpeechRecognitionHttpExample {


    public static void main(String[] args) throws InterruptedException, IOException {
        //GlobalConfig.ifLog = true;  //是否开启sdk日志打印
        //SpeechRecognitionSysConfig.ifSyncHttp=true; //指定Http实现方式 true:同步 false:异步  (默认异步)
        //SpeechRecognitionSysConfig.socketTimeout=0;  // 指定httpClient socket超时时间
        //SpeechRecognitionSysConfig.connectTimeout=0; // 指定httpClient 连接超时时间
        SpeechRecognitionSysConfig.requestWay = AsrConstant.RequestWay.Http;//配置请求方式 默认为websocket ,http需要特殊指定

        //从配置文件读取密钥（可自行修改）
        Properties props = new Properties();
        //从配置文件读取密钥
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");

        //1.创建client实例 client为单例
        SpeechClient speechClient = SpeechClient.newInstance(appId, secretId, secretKey);


        //2.创建SpeechRecognizerRequest,这里配置请求相关参数包含切片大、引擎模型类型、文件格式等
        SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
        request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
        request.setVoiceFormat(1);            //指定音频格式


        //案例使用文件模拟实时获取语音流，用户使用可直接调用write(data)传入字节数据
        FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
        List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 6400);


        //3.创建SpeechRecognizer实例，该实例是语音识别的处理者。
        SpeechRecognizer speechRecognizer = speechClient.newSpeechRecognizer(request, new MySpeechRecognitionListener());
        //开始发送音频，调用start方法
        speechRecognizer.start();
        //发送音频
        for (int i = 0; i < speechData.size(); i++) {
            //模拟音频间隔
            Thread.sleep(100);
            speechRecognizer.write(speechData.get(i));
        }
        //发送音频结束，调用stop方法
        speechRecognizer.stop();


        fileInputStream.close();
    }


    /**
     * 回调方法
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
