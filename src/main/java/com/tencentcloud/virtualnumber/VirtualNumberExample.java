package com.tencentcloud.virtualnumber;

import com.tencent.SpeechClient;
import com.tencent.asr.model.VirtualNumberRequest;
import com.tencent.asr.model.VirtualNumberResponse;
import com.tencent.asr.model.VirtualNumberServerConfig;
import com.tencent.asr.service.VirtualNumberRecognitionListener;
import com.tencent.asr.service.VirtualNumberRecognizer;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * 虚拟号案例
 */
public class VirtualNumberExample {

    public static void main(String[] args) throws IOException {
        GlobalConfig.ifLog = false; //是否打印日志 用于问题排查 默认false
        //自定义日志拦截器 默认为sout
        //GlobalConfig.sdkLogInterceptor = new MySdkLogInterceptor();

        //密钥获取地址见README.md
        //配置文件读取密钥
        Properties props = new Properties();
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");

        //用于标识回调 自定义随机字符串
        String listenerId = UUID.randomUUID().toString();
        ExampleListener listener = new ExampleListener(listenerId);

        //读取音频数据,这里为了演示方便直接保存为字节数组
        FileInputStream fileInputStream = new FileInputStream(new File("test.wav"));
        List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);

        // 初始化request
        VirtualNumberRequest request = new VirtualNumberRequest();
        request.setAppId(Integer.valueOf(appId)); //必填
        request.setSecretId(secretId);//必填
        request.setSecretKey(secretKey);//必填
        request.setVoiceId(UUID.randomUUID().toString());//必填
        request.setVoiceFormat(12);//必填
        request.setWaitTime(60);
        //初始化VirtualNumberRecognizer
        //一次识别对应一个VirtualNumberRecognizer 切勿重复使用
        //VirtualNumberServerConfig.getInstance()默认全局唯一，可自定义
        VirtualNumberRecognizer recognizer = SpeechClient
                .newVirtualNumberRecognizer(VirtualNumberServerConfig.getInstance(), request, listener);
        try {
            //开启连接 异常则抛出异常
            recognizer.start();
            for (int i = 0; i < speechData.size(); i++) {
                Thread.sleep(20);//模拟语音流
                boolean end = recognizer.write(speechData.get(i));
                if (end) {
                    break;
                }
            }
            recognizer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //自定义listener回调用于接收响应数据
    static class ExampleListener extends VirtualNumberRecognitionListener {

        /**
         * 初始化
         *
         * @param id 用于区分回调
         */
        public ExampleListener(String id) {
            super(id);
        }

        /**
         * 开始响应
         *
         * @param response 结果
         */
        @Override
        public void onRecognitionStart(VirtualNumberResponse response) {
            System.out.println(JsonUtil.toJson(response));
        }

        /**
         * 结束响应
         *
         * @param response 结果
         */
        @Override
        public void onRecognitionComplete(VirtualNumberResponse response) {
            System.out.println(JsonUtil.toJson(response));
        }

        /**
         * 失败响应
         *
         * @param response 结果
         */
        @Override
        public void onFail(VirtualNumberResponse response) {
            System.out.println(JsonUtil.toJson(response));
        }
    }

}
