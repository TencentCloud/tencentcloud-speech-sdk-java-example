
package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.SpeechRecognitionRequest;
import com.tencent.asr.model.SpeechRecognitionSysConfig;
import com.tencent.asr.model.SpeechWebsocketConfig;
import com.tencent.asr.service.SpeechRecognizer;
import com.tencent.core.utils.ByteUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

public class SpeechRecognition {

    static {
        /*//是否打印日志
        GlobalConfig.ifLog = true;
        //自定义日志拦截器
        GlobalConfig.sdkLogInterceptor = new SdkLogInterceptor();
        //SpeechRecognitionSysConfig语音识别全局默认配置文件,默认值可通过SpeechRecognitionSysConfig配置
        //speechWsRecognizer.start()方法执行超时时间单位为s,对应首包时延一般为百ms,具体根据网络情况而定，一般情况可不用调整
        SpeechRecognitionSysConfig.wsStartMethodWait = 1;
        //speechWsRecognizer.stop()方法执行超时时间单位为s,对应尾包时延
        SpeechRecognitionSysConfig.wsStopMethodWait = 1;
        //OkHttpClient相关默认配置
        //设置socket连接超时时间
        SpeechRecognitionSysConfig.wsConnectTimeOut = 10000;
        //设置数据写入连接超时时间
        SpeechRecognitionSysConfig.wsWriteTimeOut = 10000;
        //设置数据读取连接超时时间
        SpeechRecognitionSysConfig.wsReadTimeOut = 10000;
        //连接池中连接的最大时长
        SpeechRecognitionSysConfig.wsKeepAliveDuration = 300000;
        //连接池大小
        SpeechRecognitionSysConfig.wsMaxIdleConnections = 200;
        //当前okhttpclient实例最大的并发请求数
        SpeechRecognitionSysConfig.wsMaxRequests = 10;*/
    }

    /**
     * 自定义配置信息可参考resetConfigExample
     */
    public static void resetConfigExample() throws IOException {
        Properties props = new Properties();
        //从配置文件读取密钥
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        // websocket使用okhttp框架实现，如需自定义相关配置可修改SpeechWebsocketConfig配置信息
        // 具体配置见SpeechWebsocketConfig
        SpeechWebsocketConfig config = SpeechWebsocketConfig.init();
        config.setExecutorService(Executors.newFixedThreadPool(1)); //对应okhttpclient线程池
        config.setWsMaxRequests(100); //对应okhttpclient maxRequests配置

        SpeechClient speechClient = SpeechClient.newInstance(appId, secretId, secretKey, "", config);
        runOnce(speechClient);
    }

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
        props.load(new FileInputStream("../../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        return SpeechClient.newInstance(appId, secretId, secretKey);
    }

    /**
     * 并发
     *
     * @param client SpeechClient
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
            Thread.sleep(600000);
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
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream,
                    SpeechRecognitionSysConfig.requestWay == AsrConstant.RequestWay.Http ? 6400 : 640);
            //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
            request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
            request.setVoiceFormat(1);  //指定音频格式

            //⚠️支持扩展参数 对于sdk未及时更新参数可通过扩展参数调用
            //Map<String,Object> ext=new HashMap<>();
            //ext.put("reinforce_hotword",1);
            //request.setExtendsParam(ext);

            SpeechRecognizer speechWsRecognizer = client
                    .newSpeechRecognizer(request, new MySpeechRecognitionListener());
            //开始识别 调用start方法
            boolean success = speechWsRecognizer.start();
            if (success) {
                for (int i = 0; i < speechData.size(); i++) {
                    //模拟音频间隔
                    Thread.sleep(SpeechRecognitionSysConfig.requestWay == AsrConstant.RequestWay.Http ? 200 : 20);
                    //发送数据
                    speechWsRecognizer.write(speechData.get(i));
                }
                //结束识别调用stop方法
                speechWsRecognizer.stop();
            }
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
