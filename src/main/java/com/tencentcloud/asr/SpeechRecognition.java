
package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.model.SpeechRecognitionRequest;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.core.utils.ByteUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class SpeechRecognition {

    /**
     * 获取 SpeechClient
     *
     * @return SpeechClient
     * @throws IOException IOException
     */
    public static SpeechClient getSpeechClient() throws IOException {
        //从配置文件读取密钥（可自行修改）
        Properties props = new Properties();
        //从配置文件读取密钥
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        return SpeechClient.newInstance(appId, secretId, secretKey);
    }

    /**
     * 并发
     *
     * @param client    SpeechClient
     * @param threadNum 线程数
     * @throws InterruptedException InterruptedException
     */
    public static void runConcurrency(final SpeechClient client, int threadNum) throws InterruptedException {
        while (true) {
            for (int i = 0; i < threadNum; i++) {
                Thread.sleep(50);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnce(client);
                    }
                }).start();
            }
            Thread.sleep(60000);
        }
    }

    /**
     * 单独执行
     *
     * @param client SpeechClient
     */
    public static void runOnce(final SpeechClient client) {
        try {
            //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k.wav"));
            //http 建议每次传输200ms数据   websocket建议每次传输40ms数据
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
            request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
            request.setVoiceFormat(1);  //指定音频格式
            SpeechRecognizer speechWsRecognizer = client.newSpeechRecognizer(request, new MySpeechRecognitionListener());
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
