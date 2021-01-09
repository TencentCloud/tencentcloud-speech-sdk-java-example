
package com.tencentcloud.asr;

import com.tencent.SpeechClient;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.*;
import com.tencent.core.model.GlobalConfig;

import java.io.IOException;

/**
 * http 语音实时识别参考案例
 */
public class SpeechRecognitionHttpExample extends SpeechRecognition {

    /**
     * 语音实时识别
     *
     * @param args args
     * @throws InterruptedException InterruptedException
     * @throws IOException          IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        Boolean runOnce = true;
        int threadNum = 10;
        SpeechRecognitionSysConfig.requestWay = AsrConstant.RequestWay.Http;//配置请求方式 默认为websocket
        SpeechClient client = getSpeechClient();
        if (runOnce) {
            runOnce(client);
        } else {
            runConcurrency(client, threadNum);
        }

    }
}
