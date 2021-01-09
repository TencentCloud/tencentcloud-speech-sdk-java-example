
package com.tencentcloud.asr;

import cn.hutool.core.util.RandomUtil;
import com.tencent.asr.constant.AsrConstant;
import com.tencent.asr.model.*;
import com.tencent.asr.service.AsrClient;
import com.tencent.core.handler.BaseEventListener;
import com.tencent.core.handler.RealTimeEventListener;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.service.TCall;
import com.tencent.core.utils.ByteUtils;
import com.tencent.core.utils.JsonUtil;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * 该案例已废弃，适用版本(1.0.0,1.0.1)
 */
@Deprecated
public class SpeechRecognitionHttpByteArrayExample {

    /**
     * 语音实时识别
     *
     * @param args args
     * @throws InterruptedException InterruptedException
     * @throws IOException          IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        GlobalConfig.ifLog=true;
        Properties props = new Properties();
        //从配置文件读取密钥
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");
        final AsrConfig config = AsrConfig.builder()
                .appId(appId).secretId(secretId)
                .secretKey(secretKey)
                .build();
        //Thread.sleep(30000);
        while (true) {
            for (int i = 0; i < RandomUtil.randomInt(4); i++) {
                Thread.sleep(100);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        //请求参数，用于配置语音识别相关参数，可使用init方法进行默认配置或使用 builder的方式构建自定义参数
                        final AsrRequest request = AsrRequest.init();
                        //设置切片长度 建议：8k  3200字节，16k 6400字节
                        request.setCutLength(6400);
                        //request.setNeedVad(0);
                        //设置引擎
                        request.setEngineModelType("8k_zh_finance");
                        //request.setVoiceFormat(8);
                        //设置语音编码方式 语音编码方式，可选，默认值为 4。1：wav(pcm)；4：speex(sp)；6：silk；8：mp3；10：opus（opus 格式音频流封装说明
                        request.setVoiceFormat(1);

                        //AsrClient 应全局唯一，保证连接池和线程池可以公用，提高服务性能
                        final AsrClient asrClient = AsrClient.newInstance(config);
                        //开启client，执行初始化资源
                        asrClient.start();
                        TCall call = asrClient.newCall(RandomUtil.randomString(8), request,
                                new BaseEventListener<AsrResponse>() {
                                    @Override
                                    public void success(AsrResponse o) {
                                        if (o.getCode() != 0) {
                                            System.out.println("---" + JsonUtil.toJson(o));
                                        }
                                    }

                                    @Override
                                    public void fail(AsrResponse asrResponse, Exception e) {
                                        if (e != null) {
                                            System.out.println(asrResponse.getVoiceId() + " " + asrResponse.getSeq() + " " + e.getMessage());
                                        }
                                        super.fail(asrResponse, e);
                                    }
                                }, new RealTimeEventListener<AsrResponse, AsrResponse>() {

                                    @Override
                                    public AsrResponse translation(AsrResponse o) {

                                        System.out.println("---" + JsonUtil.toJson(o));

                                        return null;
                                    }
                                }, AsrConstant.DataType.BYTE);
                        //对流进行处理，语音识别
                        try {
                            //案例使用文件模拟实时获取语音流，用户使用可直接调用call.execute(data)传入字节数据
                            String[] fileArray = {"test_wav/8k/8k_19s.wav", "test_wav/8k/8k.wav", "test_wav/8k/8k.wav"};
                            FileInputStream fileInputStream = new FileInputStream(new File(fileArray[RandomUtil.randomInt(3) % 3]));
                            List<byte[]> speechData = ByteUtils.subToSmallBytes(fileInputStream, 320);
                            for (byte[] item : speechData) {
                                Thread.sleep(10);
                                call.execute(item);
                            }
                            //表示流结束，关闭监控线程
                            call.end();
                            fileInputStream.close();
                            //close方法关闭系统资源
                            asrClient.close();
                        } catch (Exception e) {

                        }
                    }
                }).start();
            }
            Thread.sleep(10000);
        }
    }
}
