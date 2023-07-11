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
import java.util.List;
import java.util.Properties;
import okhttp3.OkHttpClient;

/**
 * asr websocket自定义配置
 */
public class CustomizeAsrWsExample {

    static {
        //是否打印日志
        GlobalConfig.ifLog = true;
        //自定义日志拦截器
        GlobalConfig.sdkLogInterceptor = new SdkLogInterceptor();
        //SpeechRecognitionSysConfig语音识别全局默认配置文件,默认值可通过SpeechRecognitionSysConfig配置
        //speechWsRecognizer.start()方法执行超时时间单位为s,对应首包时延一般为百ms,具体根据网络情况而定，一般情况可不用调整
        SpeechRecognitionSysConfig.wsStartMethodWait = 1;
        //speechWsRecognizer.stop()方法执行超时时间单位为s,对应尾包时延
        SpeechRecognitionSysConfig.wsStopMethodWait = 1;
        //OkHttpClient相关默认配置,可根据业务需要调整
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
        SpeechRecognitionSysConfig.wsMaxRequests = 10;
    }

    public static void main(String[] args) {
        //建议根据业务自定义OkHttpClient相关配置，目前支持两种初始化方法  注意⚠️WsClientService建议全局唯一
        //--------------------------------------------------------------------------------------------------
        //1.配置文件初始化依赖SpeechWebsocketConfig配置
        //SpeechWebsocketConfig.init()方法使用SpeechRecognitionSysConfig默认值初始化
        //SpeechWebsocketConfig speechWebsocketConfig = SpeechWebsocketConfig.init();
        //如下可自定义参数，具体见类
        //speechWebsocketConfig.setWsMaxRequests(10);
        //speechWebsocketConfig.setExecutorService(Executors.newFixedThreadPool(1));
        //WsClientService wsClientService = new WsClientService(speechWebsocketConfig);
        //--------------------------------------------------------------------------------------------------
        //2.OkHttpClient初始化
        //自己根据业务实现配置,可以自己控制OkHttpClient关闭，okHttpClient.dispatcher().executorService().shutdown();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        WsClientService wsClientService = new WsClientService(okHttpClient);
        //--------------------------------------------------------------------------------------------------

        try {
            //案例使用文件模拟实时获取语音流，用户使用可直接调用write传入字节数据
            FileInputStream fileInputStream = new FileInputStream(new File("test_wav/16k/16k_30s..wav"));
            //http 建议每次传输200ms数据   websocket建议每次传输40ms数据
            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 640);
            //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
            SpeechRecognitionRequest request = SpeechRecognitionRequest.initialize();
            request.setEngineModelType("16k_zh"); //模型类型为必传参数，否则异常
            request.setVoiceFormat(1);  //指定音频格式

            Properties props = new Properties();
            //为了避免密钥硬编码，从配置文件读取密钥
            props.load(new FileInputStream("../../../config.properties"));
            String appId = props.getProperty("appId");
            String secretId = props.getProperty("secretId");
            String secretKey = props.getProperty("secretKey");

            Credential credential = Credential.builder().appid(appId).secretKey(secretKey).secretId(secretId).build();
            SpeechRecognizer speechWsRecognizer = SpeechClient
                    .newSpeechWsRecognizer(credential, wsClientService, request, new MySpeechRecognitionListener());

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
    }
}
