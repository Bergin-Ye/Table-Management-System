package com.metal.controller;

import com.metal.common.Result;
import com.metal.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR 图片识别接口 — 上传工单图片，通义千问 Qwen3.5-OCR 提取结构化字段
 */
@RestController
@RequestMapping("/api")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/ocr/recognize")
    public Result<?> recognize(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return Result.fail("请上传图片");
        }
        // 限制 10MB
        if (image.getSize() > 10 * 1024 * 1024) {
            return Result.fail("图片大小不能超过 10MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return Result.fail("只支持图片格式");
        }

        log.info("OCR 识别请求: {} ({} bytes)", image.getOriginalFilename(), image.getSize());
        try {
            Map<String, Object> result = ocrService.recognize(image);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("OCR 识别失败", e);
            return Result.fail("OCR 识别失败: " + e.getMessage());
        }
    }
}
