package com.metal.controller;

import com.metal.common.BizException;
import com.metal.common.Result;
import com.metal.service.OcrService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/original-record")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * OCR 图片识别 — 上传工单图片，返回结构化字段自动填充表单
     */
    @PostMapping("/ocr-recognize")
    public Result<?> recognize(@RequestParam("image") MultipartFile image) {
        try {
            Map<String, Object> result = ocrService.recognize(image.getBytes());
            return Result.ok(result);
        } catch (IOException e) {
            throw new BizException("读取上传图片失败: " + e.getMessage());
        }
    }
}
