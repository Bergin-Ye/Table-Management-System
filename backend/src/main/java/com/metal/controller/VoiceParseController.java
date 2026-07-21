package com.metal.controller;

import com.metal.common.Result;
import com.metal.service.VoiceParseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoiceParseController {

    private final VoiceParseService voiceParseService;

    public VoiceParseController(VoiceParseService voiceParseService) {
        this.voiceParseService = voiceParseService;
    }

    /**
     * 语音/文字解析 — 将口语化文字拆解为表单字段
     * 请求体: { "text": "日期2026年7月21日 班次白班 ...", "table": "original-record" }
     */
    @PostMapping("/voice-parse")
    public Result<?> parse(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        String table = body.getOrDefault("table", "");
        Map<String, Object> result = voiceParseService.parse(text, table);
        return Result.ok(result);
    }
}
