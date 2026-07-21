package com.metal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通义千问 Qwen3.5-OCR 图片识别服务
 * 使用 DashScope OpenAI兼容API，将维修工单图片中的手写/印刷文字提取为结构化字段
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${ocr.dashscope.api-key:}")
    private String apiKey;

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String MODEL = "qwen3.5-ocr";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    /**
     * 识别工单图片，返回结构化字段映射
     */
    public Map<String, Object> recognize(MultipartFile image) throws IOException {
        // 1. 读取图片并转为 base64
        byte[] imageBytes = image.getBytes();
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/jpeg";
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageUrl = "data:" + contentType + ";base64," + base64Image;

        // 2. 构建请求
        String systemPrompt = buildSystemPrompt();
        String requestBody = buildRequestBody(imageUrl, systemPrompt);

        log.info("OCR 请求发送, 图片大小: {} bytes", imageBytes.length);

        // 3. 调用 API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("OCR 响应状态: {}, 长度: {}", response.statusCode(),
                    response.body() != null ? response.body().length() : 0);

            if (response.statusCode() != 200) {
                log.error("OCR API 错误: {}", response.body());
                return errorResult("OCR 服务返回错误: HTTP " + response.statusCode());
            }

            // 4. 解析响应
            return parseResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResult("OCR 请求被中断");
        } catch (IOException e) {
            log.error("OCR 请求失败", e);
            return errorResult("OCR 服务请求失败: " + e.getMessage());
        }
    }

    /**
     * 构建系统提示词 - 针对金属厂维修工单优化
     */
    private String buildSystemPrompt() {
        return "你是一个工厂维修工单OCR数据提取助手。请仔细识别图片中的所有文字（包括手写和印刷），" +
                "提取以下字段信息并以纯JSON格式返回。\n\n" +
                "字段说明（只提取图片中实际存在的字段，没有的设为空字符串\"\"）：\n" +
                "- recordDate: 日期，统一转为 YYYY-MM-DD 格式（如 2026-07-21）\n" +
                "- shift: 班次（白班 或 夜班）\n" +
                "- factory: 厂房/车间（如 A、A3、B、C）\n" +
                "- serialNumber: 序号/编号/故障维修序号\n" +
                "- machineNo: 机台号/设备编号\n" +
                "- machineModel: 机型/设备出厂编号（如 FANUC、西门子等）\n" +
                "- diagnostician: 诊断人姓名\n" +
                "- repairPerson: 维修人姓名\n" +
                "- confirmer: 确认人姓名\n" +
                "- repairRequestTime: 故障时间/报修时间，统一转为 HH:mm 格式（如 15:30，无分钟则补:00）\n" +
                "- startTime: 开始维修时间/接单时间，格式同上\n" +
                "- endTime: 维修结束时间/诊断结束时间，格式同上\n" +
                "- faultPhenomenon: 故障现象描述\n" +
                "- faultDescription: 维修描述/维修方案/故障原因分析及维修方案\n" +
                "- materialCode: 物料编码/料号/配件编码\n" +
                "- partName: 零件名称/配件名称/使用的配件\n" +
                "- quantity: 数量（纯数字，如 1、2、3）\n" +
                "- machineOnMaterial: 上机物料号（装上的配件编码，标记\"装\"或\"上机\"）\n" +
                "- machineOffMaterial: 下机物料号（拆下的配件编码，标记\"拆\"或\"下机\"）\n" +
                "- remark: 备注信息\n" +
                "- deliveryRecordRef: 送货记录引用号\n\n" +
                "重要规则：\n" +
                "1. 只提取图片中实际存在的字段，没有的设为空字符串\"\"，严禁编造\n" +
                "2. 手写文字要仔细辨认，结合上下文推断，但不确定的就留空\n" +
                "3. 日期务必统一为 YYYY-MM-DD 格式（月份和日期补零）\n" +
                "4. 时间务必统一为 HH:mm 格式，如遇到\"20时00分\"转为\"20:00\"，\"20时\"转为\"20:00\"\n" +
                "5. 中文姓名只取2-3个字的人名，不要带职务或括号\n" +
                "6. 配件编码要完整，包括前缀和数字（如 26T3-0467、J524111330）\n" +
                "7. 返回纯JSON对象，不要用markdown代码块```json```包裹，不要加任何解释文字\n" +
                "8. 如果图片中有\"装：XXX\"和\"拆：YYY\"，分别填入上机物料号和下机物料号";
    }

    /**
     * 构建请求体 JSON
     */
    private String buildRequestBody(String imageUrl, String prompt) {
        try {
            Map<String, Object> content1 = new LinkedHashMap<>();
            content1.put("type", "image_url");
            Map<String, String> imageUrlMap = new LinkedHashMap<>();
            imageUrlMap.put("url", imageUrl);
            content1.put("image_url", imageUrlMap);

            Map<String, Object> content2 = new LinkedHashMap<>();
            content2.put("type", "text");
            content2.put("text", prompt);

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", new Object[]{content1, content2});

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", MODEL);
            body.put("messages", new Object[]{message});

            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构建OCR请求失败", e);
        }
    }

    /**
     * 解析 API 响应，提取 JSON 字段
     */
    private Map<String, Object> parseResponse(String responseBody) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                log.error("OCR 响应无 choices: {}", responseBody);
                return errorResult("OCR 返回数据格式异常");
            }

            String content = choices.get(0).get("message").get("content").asText();
            log.info("OCR 原始内容长度: {}", content != null ? content.length() : 0);

            // 尝试从内容中提取JSON对象
            Map<String, String> fields = extractJsonFields(content);
            result.put("fields", fields);

            // 计算已填充字段数
            long filledCount = fields.values().stream().filter(v -> v != null && !v.isBlank()).count();
            result.put("filledCount", (int) filledCount);
            result.put("rawText", content);

        } catch (JsonProcessingException e) {
            log.error("OCR 响应解析失败", e);
            return errorResult("OCR 响应解析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从模型返回的文本中提取JSON字段
     * 模型可能返回纯JSON、markdown包裹的JSON、或混合文本
     */
    Map<String, String> extractJsonFields(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return fields;

        // 尝试提取JSON对象
        String jsonStr = text.trim();

        // 去掉markdown代码块包裹
        Pattern mdPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");
        Matcher mdMatcher = mdPattern.matcher(jsonStr);
        if (mdMatcher.find()) {
            jsonStr = mdMatcher.group(1).trim();
        }

        // 查找第一个 { 到最后一个 }
        int start = jsonStr.indexOf('{');
        int end = jsonStr.lastIndexOf('}');
        if (start >= 0 && end > start) {
            jsonStr = jsonStr.substring(start, end + 1);
        } else {
            // 不是JSON，尝试用关键词匹配方式解析
            return parseKeywords(text);
        }

        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            root.fields().forEachRemaining(entry -> {
                String value = entry.getValue().asText();
                if (value != null && !value.isBlank()) {
                    fields.put(entry.getKey(), value.trim());
                }
            });
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败，降级为关键词匹配: {}", e.getMessage());
            return parseKeywords(text);
        }

        return fields;
    }

    /**
     * 降级方案：用关键词匹配从文本中提取字段值
     * 复用 VoiceParseService 的锚点匹配思路
     */
    private Map<String, String> parseKeywords(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        // 常见字段的关键词映射
        String[][] mappings = {
                {"日期", "recordDate"}, {"班次", "shift"}, {"厂房", "factory"}, {"车间", "factory"},
                {"机台号", "machineNo"}, {"机型", "machineModel"}, {"诊断人", "diagnostician"},
                {"维修人", "repairPerson"}, {"确认人", "confirmer"},
                {"故障时间", "repairRequestTime"}, {"报修时间", "repairRequestTime"},
                {"开始时间", "startTime"}, {"接单时间", "startTime"},
                {"维修结束时间", "endTime"}, {"诊断结束时间", "endTime"}, {"结束时间", "endTime"},
                {"故障现象", "faultPhenomenon"}, {"故障描述", "faultPhenomenon"},
                {"维修描述", "faultDescription"}, {"维修方案", "faultDescription"},
                {"故障原因分析及维修方案", "faultDescription"},
                {"物料编码", "materialCode"}, {"料号", "materialCode"}, {"配件编码", "materialCode"},
                {"配件名称", "partName"}, {"配件", "partName"}, {"使用的配件", "partName"},
                {"数量", "quantity"}, {"备注", "remark"},
                {"上机物料号", "machineOnMaterial"}, {"上机物料", "machineOnMaterial"},
                {"下机物料号", "machineOffMaterial"}, {"下机物料", "machineOffMaterial"},
                {"送货记录引用", "deliveryRecordRef"},
                // 针对"装：XXX"和"拆：YYY"的匹配
                {"装：", "machineOnMaterial"}, {"拆：", "machineOffMaterial"},
        };

        for (String[] mapping : mappings) {
            String keyword = mapping[0];
            String field = mapping[1];
            int idx = text.indexOf(keyword);
            if (idx >= 0) {
                int valueStart = idx + keyword.length();
                // 取到下一个常见分隔符
                int valueEnd = text.length();
                for (String delim : new String[]{"\n", "，", "。", "  ", "；", "\t"}) {
                    int dIdx = text.indexOf(delim, valueStart);
                    if (dIdx > 0 && dIdx < valueEnd) valueEnd = dIdx;
                }
                String value = text.substring(valueStart, Math.min(valueStart + 50, valueEnd)).trim();
                if (!value.isEmpty()) {
                    fields.put(field, value);
                }
            }
        }
        return fields;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fields", new LinkedHashMap<>());
        result.put("filledCount", 0);
        result.put("rawText", "");
        result.put("error", message);
        return result;
    }
}
