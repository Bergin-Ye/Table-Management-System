package com.metal.controller;

import com.metal.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @PostMapping("/recognize")
    public Result<?> recognize(@RequestParam("image") MultipartFile image) {
        // OCR 功能预留，后续迭代实现
        return Result.ok(Map.of(
                "message", "OCR 功能开发中，敬请期待",
                "text", "",
                "fields", java.util.Collections.emptyMap()
        ));
    }
}
