package com.tencentcloud.tts;


import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.CreateTtsTaskRequest;
import com.tencentcloudapi.tts.v20190823.models.CreateTtsTaskResponse;
import com.tencentcloudapi.tts.v20190823.models.DescribeTtsTaskStatusRequest;
import com.tencentcloudapi.tts.v20190823.models.DescribeTtsTaskStatusResponse;

/**
 * 长文本语音合成案例
 */
public class TtsTaskExample {

    public static void main(String[] args) {
        try {
            String appId = "your appid";
            String secretId = "your secretId";
            String secretKey = "your secretKey";

            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            TtsClient client = new TtsClient(cred, "", clientProfile);
            CreateTtsTaskRequest request = new CreateTtsTaskRequest();
            request.setText("腾讯云语音合成测试");
            request.setModelType(1L);
            request.setVolume(0.0f);
            request.setSpeed(0.0f);
            request.setProjectId(1L);
            request.setVoiceType(101003L);
            request.setSampleRate(16000L);
            request.setCodec("wav");
            request.setPrimaryLanguage(1L);
            //回调接口
            //request.setCallbackUrl("https://xxxxxx.com/xxxx/callback");
            //创建任务
            CreateTtsTaskResponse response = client.CreateTtsTask(request);
            String taskId = response.getData().getTaskId();

            //建议采用回调的方式获取任务结果
            //此处采用轮训实现任务结果查询
            while (true) {
                Thread.sleep(10000);
                System.out.println("query task status");
                DescribeTtsTaskStatusRequest statusRequest = new DescribeTtsTaskStatusRequest();
                statusRequest.setTaskId(taskId);
                DescribeTtsTaskStatusResponse statusResponse = client.DescribeTtsTaskStatus(statusRequest);
                if (statusResponse.getData().getStatus() >= 2) {
                    if (statusResponse.getData().getStatus() == 2) {
                        //task success do something
                        System.out.println(statusResponse.getData().getResultUrl());
                        break;
                    }
                    if (statusResponse.getData().getStatus() == 3) {
                        //task fail do something
                        System.out.println(statusResponse.getData().getErrorMsg());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            //exception do something
            e.printStackTrace();
        }
    }
}
