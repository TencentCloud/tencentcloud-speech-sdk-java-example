package com.tencentcloud.asrasync;


import com.google.gson.Gson;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.*;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 异步版语音识别
 *
 * 依赖sdk https://github.com/TencentCloud/tencentcloud-sdk-java
 * <dependency>
 * <groupId>com.tencentcloudapi</groupId>
 * <artifactId>tencentcloud-sdk-java</artifactId>
 * <version>最新版本</version>
 * </dependency>
 */
public class SpeechAsyncRecognizerExample {


    /**
     * 异步版语音识别 demo
     *
     * @param args args
     * @throws IOException IOException
     */
    public static void main(String[] args) throws IOException {
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        String appId = "your_appid";
        String secretId = "your secretId";
        String secretKey = "your secretKey";

        //配置请求参数
        CreateAsyncRecognitionTaskRequest req = new CreateAsyncRecognitionTaskRequest();
        req.setEngineType("16k_zh_video");
        //url 配置自己的直播流地址
        req.setUrl("rtmp://127.0.0.1:1935/live/qwe");
        //CallbackUrl 配置自己的回调地址
        req.setCallbackUrl("https://xxxxxx.com/xxxx/callback");

        Credential credential = new Credential(secretId, secretKey);
        AsrClient client = new AsrClient(credential, "ap-shanghai");
        try {
            //创建异步识别任务
            CreateAsyncRecognitionTaskResponse response = client.CreateAsyncRecognitionTask(req);
            Gson gson = new Gson();
            System.out.println(gson.toJson(response));

            //关闭异步识别任务
            CloseAsyncRecognitionTaskRequest closeAsyncRecognitionTaskRequest = new CloseAsyncRecognitionTaskRequest();
            closeAsyncRecognitionTaskRequest.setTaskId(response.getData().getTaskId());
            CloseAsyncRecognitionTaskResponse closeAsyncRecognitionTaskResponse = client.CloseAsyncRecognitionTask(closeAsyncRecognitionTaskRequest);
            System.out.println(gson.toJson(closeAsyncRecognitionTaskResponse));

            //查询正在执行的异步识别任务
            DescribeAsyncRecognitionTasksRequest request = new DescribeAsyncRecognitionTasksRequest();
            DescribeAsyncRecognitionTasksResponse response1 = client.DescribeAsyncRecognitionTasks(request);
            System.out.println(gson.toJson(response1));

        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
    }
}
