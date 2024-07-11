package com.tencentcloud.ttsv2;

import com.google.gson.Gson;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.tts.utils.Ttsutils;
import com.tencent.ttsv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;

/**
 * 本接口服务采用 websocket 协议，将请求文本合成为音频，同步返回合成音频数据及相关文本信息，达到“边合成边播放”的效果。
 * 在使用该接口前，需要 开通语音合成服务，并进入 API 密钥管理页面 新建密钥，生成 AppID、SecretID 和 SecretKey，用于 API 调用时生成签名，签名将用来进行接口鉴权。
 */
public class TTSWsFlowingDemo {
    static Logger logger = LoggerFactory.getLogger(TTSWsFlowingDemo.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(TtsConstant.DEFAULT_TTS_V2_REQ_URL);

    public static void main(String[] args) throws IOException {
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        String appId = "your_appid";
        String secretId = "your secretId";
        String secretKey = "your secretKey";
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }

    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        FlowingSpeechSynthesizerRequest request = new FlowingSpeechSynthesizerRequest();
        request.setVolume(0f);
        request.setSpeed(0f);
        request.setCodec("pcm");
        request.setSampleRate(16000);
        request.setVoiceType(301032);
        request.setEnableSubtitle(true);
        request.setEmotionCategory("neutral");
        request.setEmotionIntensity(100);
        request.setSessionId(UUID.randomUUID().toString());//sessionId，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
        //request.set("SegmentRate", 1); //sdk暂未支持参数，可通过该方法设置
        logger.debug("session_id:{}", request.getSessionId());
        FlowingSpeechSynthesizerListener listener = new FlowingSpeechSynthesizerListener() {//tips：回调方法中应该避免进行耗时操作，如果有耗时操作建议进行异步处理否则会影响websocket请求处理
            byte[] audio = new byte[0];

            @Override
            public void onSynthesisStart(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisStart", response.getSessionId(), new Gson().toJson(response));
            }

            @Override
            public void onSynthesisEnd(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisEnd", response.getSessionId(), new Gson().toJson(response));
                if ("pcm".equals(request.getCodec())) {
                    Ttsutils.responsePcm2Wav(16000, audio, request.getSessionId());
                }
                if ("mp3".equals(request.getCodec())) {
                    try {
                        FileOutputStream out = new FileOutputStream("./test301021.mp3", false);
                        out.write(audio);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onAudioResult(ByteBuffer buffer) {
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                audio = ByteUtils.concat(audio, data);
            }

            @Override
            public void onTextResult(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onTextResult", response.getSessionId(), new Gson().toJson(response));
            }

            /**
             * 错误回调 当发生错误时回调该方法
             * @param response 响应
             */
            @Override
            public void onSynthesisFail(SpeechSynthesizerResponse response) {
                logger.info("{} session_id:{},{}", "onSynthesisFail", response.getSessionId(), new Gson().toJson(response));
            }
        };
        //synthesizer不可重复使用，每次合成需要重新生成新对象
        FlowingSpeechSynthesizer synthesizer = null;
        String[] texts = {"五位壮士一面向顶峰攀登，一面依托大树和",
                "岩石向敌人射击。山路上又留下了许多具敌",
                "人的尸体。到了狼牙山峰顶，五壮士居高临",
                "下，继续向紧跟在身后的敌人射击。不少敌人",
                "坠落山涧，粉身碎骨。班长马宝玉负伤了，子",
                "弹都打完了，只有胡福才手里还剩下一颗手榴",
                "弹，他刚要拧开盖子，马宝玉抢前一步，夺过",
                "手榴弹插在腰间，他猛地举起一块磨盘大的石",
                "头，大声喊道：“同志们！用石头砸！”顿时，",
                "石头像雹子一样，带着五位壮士的决心，带着",
                "中国人民的仇恨，向敌人头上砸去。山坡上传",
                "来一阵叽里呱啦的叫声，敌人纷纷滚落深谷。"
        };
        try {
            synthesizer = new FlowingSpeechSynthesizer(proxy, credential, request, listener);
            long currentTimeMillis = System.currentTimeMillis();
            synthesizer.start();
            logger.info("synthesizer start latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
            for (String text : texts) {
                synthesizer.process(text);
                Thread.sleep(500);
            }
            currentTimeMillis = System.currentTimeMillis();
            synthesizer.stop();
            logger.info("synthesizer stop latency : " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (synthesizer != null) {
                synthesizer.close(); //关闭连接
            }
        }
    }
}
