package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.Credential;
import com.tencent.asr.model.SpeechRecognitionRequest;
import com.tencent.asr.model.SpeechRecognitionSysConfig;
import com.tencent.asr.model.SpeechWebsocketConfig;
import com.tencent.asr.service.SdkRunException;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.asr.service.WsClientService;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.service.SdkLogInterceptor;
import com.tencent.core.utils.ByteUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;

/**
 * asr websocket自定义配置
 */
public class ConcurrentAsrWsExample {

    private static final int MAX_REQUEST_COUNT = 2;

    static WsClientService wsClientService;

    static {
        //是否打印日志
        GlobalConfig.ifLog = true;
        //自定义日志拦截器
        GlobalConfig.sdkLogInterceptor = new SdkLogInterceptor();
        wsClientService = new WsClientService(SpeechWebsocketConfig.init());

    }


    public static void doConcurrency() throws IOException {
        //创建CountDownLatch并设置计数值，该count值可以根据线程数的需要设置
        CountDownLatch countDownLatch = new CountDownLatch(MAX_REQUEST_COUNT);
        //创建线程池
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        Properties props = new Properties();
        //为了避免密钥硬编码，从配置文件读取密钥
        props.load(new FileInputStream("../../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        final Credential credential = Credential.builder().appid(appId).secretKey(secretKey).secretId(secretId).build();
        for (int num = 0; num < MAX_REQUEST_COUNT; num++) {
            cachedThreadPool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " do something!");
                    try {
                        //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
                        FileInputStream fileInputStream = new FileInputStream(new File("test_wav/8k/8k_19s.wav"));
                        //http 建议每次传输200ms数据   websocket建议每次传输40ms数据
                        List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
                        //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
                        SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
                        request.setEngineModelType("8k_zh"); //模型类型为必传参数，否则异常
                        request.setVoiceFormat(1);  //指定音频格式
                        SpeechRecognizer speechWsRecognizer = SpeechClient
                                .newSpeechWsRecognizer(credential, wsClientService, request,
                                        new MySpeechRecognitionListener());
                        //开始识别 调用start方法
                        speechWsRecognizer.start();
                        for (int i = 0; i < speechData.size(); i++) {
                            //模拟音频间隔
                            Thread.sleep(20);
                            //发送数据
                            try {
                                speechWsRecognizer.write(speechData.get(i));
                            } catch (SdkRunException e) {
                                //服务端返回400x错误,服务端断开连接,错误信息见回调onFail
                                System.out.println("write:" + e.getCode() + "｜｜" + e.getMessage());
                                break;
                            }
                        }
                        //结束识别调用stop方法
                        speechWsRecognizer.stop();
                        fileInputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("Exception: do something exception");
                } finally {
                    //该线程执行完毕-1
                    countDownLatch.countDown();
                }
            });
        }
        System.out.println("main thread do something-1");
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            System.out.println("Exception: await interrupted exception");
        } finally {
            System.out.println("countDownLatch: " + countDownLatch.toString());
        }
        System.out.println("main thread do something-2");
    }

    public static void main(String[] args) {
        try {
            while (true) {
                doConcurrency();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
