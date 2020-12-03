package com.tencentcloud.tts;

import com.tencent.SpeechClient;
import com.tencent.core.model.GlobalConfig;
import com.tencent.core.utils.ByteUtils;
import com.tencent.tts.model.SpeechSynthesisRequest;
import com.tencent.tts.model.SpeechSynthesisResponse;
import com.tencent.tts.service.SpeechSynthesisListener;
import com.tencent.tts.service.SpeechSynthesizer;
import com.tencent.tts.utils.LineSplitUtils;
import com.tencent.tts.utils.Ttsutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LongTextTtsExample {
    private static String codec = "pcm";
    private static int sampleRate = 16000;

    public static void main(String[] args) throws IOException {
        GlobalConfig.ifLog = true;
        //从配置文件读取密钥
        Properties props = new Properties();
        props.load(new FileInputStream("../../config.properties"));
        String appId = props.getProperty("appId");
        String secretId = props.getProperty("secretId");
        String secretKey = props.getProperty("secretKey");


        //创建SpeechSynthesizerClient实例，目前是单例
        SpeechClient client = SpeechClient.newInstance(appId, secretId, secretKey);
        //初始化SpeechSynthesizerRequest，SpeechSynthesizerRequest包含请求参数
        SpeechSynthesisRequest request = SpeechSynthesisRequest.initialize();
        request.setCodec(codec);
        //request.setSampleRate(sampleRate);
        //request.setVolume(10);
        //request.setSpeed(2f);
        //request.setVoiceType(null);
        //request.setPrimaryLanguage(2);
        //request.setSampleRate(null);
        //使用客户端client创建语音合成实例
        SpeechSynthesizer speechSynthesizer = client.newSpeechSynthesizer(request, new SpeechSynthesisListener(){

            @Override
            public void onComplete(SpeechSynthesisResponse response) {
                Ttsutils.responsePcm2Wav(sampleRate, response.getAudio(), response.getSessionId());
            }

            @Override
            public void onMessage(byte[] data) {
            }

            @Override
            public void onFail(SpeechSynthesisResponse exception) {

            }
        });
        //执行语音合成
        String ttsText = "我从乡下跑到京城里，一转眼已经六年了。其间耳闻目睹的所谓国家大事，算起来也很不少；但在我心里，都不留什么痕迹，倘要我寻出这些事的影响来说，便只是增长了我的坏脾气，——老实说，便是教我一天比一天的看不起人。\n" +
                "　　但有一件小事，却于我有意义，将我从坏脾气里拖开，使我至今忘记不得。\n" +
                "　　这是民国六年的冬天，大北风刮得正猛，我因为生计关系，不得不一早在路上走。一路几乎遇不见人，好容易才雇定了一辆人力车，教他拉到S门去。不一会，北风小了，路上浮尘早已刮净，剩下一条洁白的大道来，车夫也跑得更快。刚近S门，忽而车把上带着一个人，慢慢地倒了。\n" +
                "　　跌倒的是一个女人，花白头发，衣服都很破烂。伊从马路上突然向车前横截过来；车夫已经让开道，但伊的破棉背心没有上扣，微风吹着，向外展开，所以终于兜着车把。幸而车夫早有点停步，否则伊定要栽一个大斤斗，跌到头破血出了。\n" +
                "　　伊伏在地上；车夫便也立住脚。我料定这老女人并没有伤，又没有别人看见，便很怪他多事，要自己惹出是非，也误了我的路。\n" +
                "　　我便对他说，“没有什么的。走你的罢！”\n" +
                "　　车夫毫不理会，——或者并没有听到，——却放下车子，扶那老女人慢慢起来，搀着臂膊立定，问伊说：\n" +
                "　　“你怎么啦？”\n" +
                "　　“我摔坏了。”\n" +
                "　　我想，我眼见你慢慢倒地，怎么会摔坏呢，装腔作势罢了，这真可憎恶。车夫多事，也正是自讨苦吃，现在你自己想法去。\n" +
                "　　车夫听了这老女人的话，却毫不踌躇，仍然搀着伊的臂膊，便一步一步的向前走。我有些诧异，忙看前面，是一所巡警分驻所，大风之后，外面也不见人。这车夫扶着那老女人，便正是向那大门走去。\n" +
                "　　我这时突然感到一种异样的感觉，觉得他满身灰尘的后影，刹时高大了，而且愈走愈大，须仰视才见。而且他对于我，渐渐的又几乎变成一种威压，甚而至于要榨出皮袍下面藏着的“小”来。\n" +
                "　　我的活力这时大约有些凝滞了，坐着没有动，也没有想，直到看见分驻所里走出一个巡警，才下了车。\n" +
                "　　巡警走近我说，“你自己雇车罢，他不能拉你了。”\n" +
                "　　我没有思索的从外套袋里抓出一大把铜元，交给巡警，说，“请你给他……”\n" +
                "　　风全住了，路上还很静。我走着，一面想，几乎怕敢想到自己。以前的事姑且搁起，这一大把铜元又是什么意思？奖他么？我还能裁判车夫么？我不能回答自己。\n" +
                "　　这事到了现在，还是时时记起。我因此也时时煞了苦痛，努力的要想到我自己。几年来的文治武力，在我早如幼小时候所读过的“子曰诗云”⑵一般，背不上半句了。独有这一件小事，却总是浮在我眼前，有时反更分明，教我惭愧，催我自新，并且增长我的勇气和希望。";
        speechSynthesizer.synthesisLongText(ttsText);
    }

}
