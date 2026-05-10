package com.nozc.zcaiagent.demo.invoke;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 阿里云灵积 HTTP 调用示例
 */
public class HttpAiInvoke {

    public static void main(String[] args) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
//        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String apiKey =TestApiKey.API_KEY;

        JSONArray messages = new JSONArray()
                .set(JSONUtil.createObj()
                        // 系统预设
                        .set("role", "system")
                        .set("content", "You are a helpful assistant."))
                .set(JSONUtil.createObj()
                        // 用户预设
                        .set("role", "user")
                        .set("content", "你是谁？"));

        JSONObject root = JSONUtil.createObj()
                .set("model", "qwen-plus")
                .set("input", JSONUtil.createObj().set("messages", messages))
                .set("parameters", JSONUtil.createObj().set("result_format", "message"));

        String responseBody = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(root.toString())
                .execute()
                .body();

        System.out.println(responseBody);
    }

}
