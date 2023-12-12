**实时语音识别SDK说明文档**

# 1.SDK使用

## SDK类说明
* GlobalConfig： 全局配置，可以修改 ifLog、sdkLogInterceptor字段配置SDK日志
```
static {
        //是否打印日志
        GlobalConfig.ifLog = true;
        //自定义日志拦截器
        GlobalConfig.sdkLogInterceptor = new SdkLogInterceptor();
}
```

* SpeechRecognitionSysConfig： 识别请求相关默认配置类，包含readtimeout、writetimeout、connecttimeout等默认值，用于httpClient、okhttpClient配置
```$xslt
static {  
        //方法超时单位 默认为秒
        SpeechRecognitionSysConfig.wsMethodWaitTimeUnit= TimeUnit.MILLISECONDS;
        
        //speechWsRecognizer.start()方法执行超时时间单位为s,对应首包时延一般为百ms,
        //具体根据网络情况而定，一般情况可不用调整
        //建议该值需大于wsConnectTimeOut 连接超时时间
        SpeechRecognitionSysConfig.wsStartMethodWait = 3;     

        //speechWsRecognizer.stop()方法执行超时时间单位为s,对应尾包时延
        SpeechRecognitionSysConfig.wsStopMethodWait = 1;    

        //OkHttpClient相关默认配置,可根据业务需要调整
        //设置socket连接超时时间
        SpeechRecognitionSysConfig.wsConnectTimeOut = 3000;
        //设置数据写入连接超时时间
        SpeechRecognitionSysConfig.wsWriteTimeOut = 1000;
        //设置数据读取连接超时时间
        SpeechRecognitionSysConfig.wsReadTimeOut = 1000;
        //连接池中连接的最大时长
        SpeechRecognitionSysConfig.wsKeepAliveDuration = 300000;
        //连接池大小
        SpeechRecognitionSysConfig.wsMaxIdleConnections = 200;
        //当前okhttpclient实例最大的并发请求数
        SpeechRecognitionSysConfig.wsMaxRequests = 10;
}
```
* SpeechWebsocketConfig： websocket配置类，如果不配置则使用SpeechRecognitionSysConfig默认值。

* WsClientService： websocketClient封装类，如果需要自定义websocket请求的client的相关信息可使用该类，一般可不用，注意该类使用时需要保持单例全局唯一。

```$xslt
static WsClientService wsClientService;
static {
        wsClientService = new WsClientService(SpeechWebsocketConfig.init());

}
```
## 案例参考说明
MySpeechRecognitionListener.java 语音识别websocket实时识别回调

CustomizeAsrWsExample.java 案例用于语音识别websocket支持多账号场景

ConcurrentAsrWsExample.java 案例用于语音识别websocket并发场景示例

PrivateExample.java 案例用于私有化部署场景

SpeechRecognitionWebsocketExample.java 案例用于语音识别websocket示例

SpeechRecognitionHttpExample.java 案例用于语音识别http示例(废弃)





