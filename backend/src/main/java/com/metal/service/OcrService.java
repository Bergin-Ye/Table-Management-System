package com.metal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metal.config.OcrConfig;
import com.metal.common.BizException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OcrService {

    private final OcrConfig ocrConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OcrService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 公开方法 ====================

    /**
     * OCR 识别图片，返回结构化字段
     */
    public Map<String, Object> recognize(byte[] imageBytes) {
        try {
            // 1. 调用阿里云 OCR
            String responseJson = callOcrApi(imageBytes);
            JsonNode root = objectMapper.readTree(responseJson);

            // 2. 提取文字块
            JsonNode blocks = root.at("/Data/SubImages/0/BlockInfo/BlockDetails");
            String fullText = root.path("Data").path("Content").asText("");

            if (blocks.isEmpty()) {
                throw new BizException("OCR 识别失败，未返回文字块");
            }

            // 3. 文字块 → 字段映射
            Map<String, String> fields = extractFields(blocks);

            // 4. 构造返回
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fields", fields);
            result.put("rawText", fullText);
            result.put("blockCount", blocks.size());
            return result;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("OCR 识别异常: " + e.getMessage());
        }
    }

    // ==================== API 调用 ====================

    private String callOcrApi(byte[] imageBytes) throws Exception {
        String now = ZonedDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String nonce = UUID.randomUUID().toString();
        String hashedBody = sha256Hex(imageBytes);

        // 构建请求头
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", ocrConfig.getEndpoint());
        headers.put("x-acs-version", "2021-07-07");
        headers.put("x-acs-action", "RecognizeAllText");
        headers.put("x-acs-date", now);
        headers.put("x-acs-signature-nonce", nonce);
        headers.put("x-acs-content-sha256", hashedBody);
        headers.put("content-type", "application/octet-stream");

        // ACS3 签名
        String auth = signRequest(headers, imageBytes);
        headers.put("Authorization", auth);

        // 构建 HTTP 请求（host 头由 HttpClient 自动管理，不能手动设置）
        String query = "Type=Advanced";
        String url = "https://" + ocrConfig.getEndpoint() + "/?" + query;

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
                .timeout(Duration.ofSeconds(30));

        headers.forEach((k, v) -> {
            if (!"host".equalsIgnoreCase(k)) {
                requestBuilder.header(k, v);
            }
        });

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new BizException("OCR 接口返回错误: HTTP " + response.statusCode() + " " + response.body());
        }

        return response.body();
    }

    // ==================== ACS3-HMAC-SHA256 签名 ====================

    private String signRequest(Map<String, String> headers, byte[] body) throws Exception {
        // 排序 header key
        List<String> sortedNames = new ArrayList<>(headers.keySet());
        Collections.sort(sortedNames);

        String signedHeaders = String.join(";", sortedNames);

        StringBuilder canonicalHeaders = new StringBuilder();
        for (String name : sortedNames) {
            canonicalHeaders.append(name).append(":").append(headers.get(name).trim()).append("\n");
        }

        String hashedPayload = sha256Hex(body);

        String canonicalRequest = "POST\n/\nType=Advanced\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedPayload;

        String hashedCanonical = sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String stringToSign = "ACS3-HMAC-SHA256\n" + hashedCanonical;
        String signature = hmacSha256Hex(ocrConfig.getAccessKeySecret(), stringToSign);

        return "ACS3-HMAC-SHA256 Credential=" + ocrConfig.getAccessKeyId()
                + ",SignedHeaders=" + signedHeaders
                + ",Signature=" + signature;
    }

    // ==================== 字段提取 ====================

    /**
     * 标签锚点匹配：在文字块数组中找标签，取其后非标签块作为值。
     * 策略：
     *   - "确认人"出现多次 → 取最后一次
     *   - "结束时间"区分诊断结束/维修结束 → 优先维修结束时间
     *   - 涉及表格行（数量/编码/部件名），走表格策略
     */
    private Map<String, String> extractFields(JsonNode blocks) {
        // 先收集所有文字块
        List<TextBlock> blockList = new ArrayList<>();
        for (JsonNode b : blocks) {
            blockList.add(new TextBlock(
                    b.path("BlockContent").asText(""),
                    b.path("BlockConfidence").asInt(0)
            ));
        }

        Map<String, String> result = new LinkedHashMap<>();

        // === 单值标签：标签后第一个有效文字块 ===
        Map<String, String[]> singleLabels = new LinkedHashMap<>();
        singleLabels.put("machineNo", new String[]{"机台号"});
        singleLabels.put("repairRequestTime", new String[]{"报障时间", "报修时间"});
        singleLabels.put("startTime", new String[]{"接单时间"});
        singleLabels.put("faultPhenomenon", new String[]{"故障描述", "故障现象"});
        singleLabels.put("faultDescription", new String[]{"故障原因", "分析及维修"});

        for (Map.Entry<String, String[]> entry : singleLabels.entrySet()) {
            String value = findValueAfterLabel(blockList, entry.getValue(), false);
            if (value != null) result.put(entry.getKey(), value);
        }

        // === 多值标签（取最后一次出现）===
        Map<String, String[]> lastOccurrenceLabels = new LinkedHashMap<>();
        lastOccurrenceLabels.put("confirmer", new String[]{"确认人"});
        lastOccurrenceLabels.put("diagnostician", new String[]{"诊断人"});
        lastOccurrenceLabels.put("repairPerson", new String[]{"维修人"});
        lastOccurrenceLabels.put("factory", new String[]{"厂房", "车间"});

        for (Map.Entry<String, String[]> entry : lastOccurrenceLabels.entrySet()) {
            String value = findValueAfterLabel(blockList, entry.getValue(), true);
            if (value != null) result.put(entry.getKey(), value);
        }

        // === 结束时间：优先"维修结束时间"，其次"诊断结束时间" ===
        String endTime = findValueAfterLabelWithPriority(blockList,
                new String[]{"维修结束时间"}, new String[]{"诊断结束时间", "结束时间"});
        if (endTime != null) result.put("endTime", endTime);

        // === 表格区域：配件编码/名称/数量 ===
        // 在"配件编码"之后的文字块区域找编码、名称、数量
        extractTableFields(blockList, result);

        return result;
    }

    /** 找标签后的第一个有效值 */
    private String findValueAfterLabel(List<TextBlock> blocks, String[] keywords, boolean lastOccurrence) {
        int foundIdx = -1;
        if (lastOccurrence) {
            for (int i = blocks.size() - 1; i >= 0; i--) {
                if (containsAny(blocks.get(i).content, keywords)) {
                    foundIdx = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < blocks.size(); i++) {
                if (containsAny(blocks.get(i).content, keywords)) {
                    foundIdx = i;
                    break;
                }
            }
        }
        if (foundIdx < 0) return null;
        return findNextValidValue(blocks, foundIdx + 1);
    }

    /** 优先级匹配：先找 primary 标签，找不到才用 fallback */
    private String findValueAfterLabelWithPriority(List<TextBlock> blocks, String[] primary, String[] fallback) {
        String val = findValueAfterLabel(blocks, primary, false);
        if (val != null) return val;
        return findValueAfterLabel(blocks, fallback, false);
    }

    /** 表格区域提取 */
    private void extractTableFields(List<TextBlock> blocks, Map<String, String> result) {
        // 找"配件编码"标签位置，表格数据在它附近
        int codeLabelIdx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).content.contains("配件编码")) {
                codeLabelIdx = i;
                break;
            }
        }
        if (codeLabelIdx < 0) return;

        // 在"配件编码"之后 10 个文字块内找看起来像编码的值（字母数字组合，长度 > 4）
        int end = Math.min(codeLabelIdx + 15, blocks.size());
        for (int i = codeLabelIdx + 1; i < end; i++) {
            String content = blocks.get(i).content;
            int conf = blocks.get(i).confidence;
            // 编码特征：包含字母和数字，长度 >= 5
            if (conf >= 70 && content.matches(".*[A-Za-z].*[0-9].*") && content.length() >= 5) {
                if (!result.containsKey("materialCode")) {
                    result.put("materialCode", content);
                }
            }
        }

        // 找"更换的部件"或"轴丝杆"取配件名称
        int partLabelIdx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            String c = blocks.get(i).content;
            if (c.contains("更换的部件") || c.contains("诊断需要维修")) {
                partLabelIdx = i;
                break;
            }
        }
        if (partLabelIdx >= 0) {
            String partName = findNextValidValue(blocks, partLabelIdx + 1);
            if (partName != null && !result.containsKey("partName")) {
                result.put("partName", partName);
            }
        }

        // 数量：在"数量"标签附近取数字型值
        for (int i = codeLabelIdx; i < end; i++) {
            if (blocks.get(i).content.equals("数量") || blocks.get(i).content.equals("数量")) {
                String qty = findNextValidValue(blocks, i + 1);
                if (qty != null && qty.matches("\\d+") && !result.containsKey("quantity")) {
                    result.put("quantity", qty);
                    break;
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    private String findNextValidValue(List<TextBlock> blocks, int startIdx) {
        for (int i = startIdx; i < blocks.size(); i++) {
            String content = blocks.get(i).content;
            // 跳过明显不是值的块
            if (isSkippable(content)) continue;
            return content;
        }
        return null;
    }

    private boolean isSkippable(String content) {
        if (content.isEmpty()) return true;
        if (content.length() <= 1 && !content.matches("\\d")) return true;
        Set<String> skip = Set.of("(", ")", "(如有)", "(填写)", "号", "称", ":", "：",
                "(BYD)", "(场内)维", "口通过", "口不通过(");
        if (skip.contains(content.trim())) return true;
        if (content.startsWith("口")) return true;
        return false;
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ==================== 加密工具 ====================

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    private String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== 内部类 ====================

    private record TextBlock(String content, int confidence) {}
}
