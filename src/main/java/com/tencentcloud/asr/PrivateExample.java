package com.tencentcloud.asr;

import cn.hutool.core.util.RandomUtil;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.AsrConfig;
import com.tencent.asr.model.SpeechRecognitionRequest;
import com.tencent.asr.model.SpeechRecognitionSysConfig;
import com.tencent.asr.model.SpeechWebsocketConfig;
import com.tencent.asr.service.SpeechHttpRecognizer;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.asr.service.SpeechWsRecognizer;
import com.tencent.asr.service.WsClientService;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * 私有化案例
 */
public class PrivateExample {

    public static void main(String[] args) {
        ws();
    }

    /**
     * 语音识别 http版本
     */
    public static void http() {
        try {
            GlobalConfig.ifLog = true;
            GlobalConfig.privateSdk = true;
            AsrConfig config = AsrConfig.builder()
                    //http://xxx.xx.xx.xx:9090/realtime_asr_private
                    .realAsrUrl("替换为私有化地址")
                    .build();
            //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
            //http 建议每次传输200ms数据   websocket建议每次传输40ms数据
            final List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 6400);
            //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
            request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
            request.setVoiceFormat(1);  //指定音频格式
            SpeechRecognizer speechWsRecognizer = new SpeechHttpRecognizer(RandomUtil.randomString(8),
                    config, request, new MySpeechRecognitionListener());
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 语音识别 ws版本
     */
    public static void ws() {
        try {
            GlobalConfig.ifLog = true;
            GlobalConfig.privateSdk = true;
            AsrConfig config = AsrConfig.builder()
                    //ws://xxx.xx.xx.xx:9090/realtime_asr_ws_private
                    .wsUrl("替换为私有化地址")
                    .build();
            //WsClientService 应保持全局唯一
            WsClientService wsClientService = new WsClientService(SpeechWebsocketConfig.init());

            //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
            //http 建议每次传输200ms数据   websocket建议每次传输40ms数据
            final List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
            request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
            request.setVoiceFormat(1);  //指定音频格式
            SpeechRecognizer speechWsRecognizer = new SpeechWsRecognizer(wsClientService, RandomUtil.randomString(8),
                    config, request, new MySpeechRecognitionListener());
            //开始识别 调用start方法
            speechWsRecognizer.start();
            for (int i = 0; i < speechData.size(); i++) {
                //模拟音频间隔
                Thread.sleep(20);
                //发送数据
                speechWsRecognizer.write(speechData.get(i));
            }
            //结束识别调用stop方法
            speechWsRecognizer.stop();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
