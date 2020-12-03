
package com.tencentcloud.asr;

import com.tencent.SpeechClient;

import java.io.IOException;

/**
 * websocket 语音实时识别参考案例
 */
public class SpeechRecognitionWebsocketExample extends SpeechRecognition {

    /**
     * 语音识别  websocket
     *
     * @param args args
     * @throws InterruptedException InterruptedException
     * @throws IOException          IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        Boolean runOnce = true;
        int threadNum = 10;
        SpeechClient client = getSpeechClient();
        if (runOnce) {
            runOnce(client);
        } else {
            runConcurrency(client, threadNum);
        }
    }


}

