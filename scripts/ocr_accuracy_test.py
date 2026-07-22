import json, base64, urllib.request, os, glob

API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
API_KEY = "sk-ws-H.EHILRPR.09wY.MEUCIQD2sGZkPAr9nUM1Mz4UAFUfNoXXF12--vepFtDEr9cPQwIgSIcnKvlw7WPLEizRq_UzhVoghF48ykNEGxHOW4OVQa0"
MODEL = "qwen3.5-ocr"

TEST_DIR = "E:/code/System/测试图片"

# 先测试简单 prompt：直接要求输出图片中所有文字
SIMPLE_PROMPT = "请仔细识别这张图片中的所有文字内容（包括手写和印刷），逐行输出你看到的文字，不要遗漏任何内容，不要添加解释。"

# 结构化 prompt
STRUCTURED_PROMPT = """你是一个工厂维修工单OCR数据提取助手。请仔细识别图片中的所有文字（包括手写和印刷），提取以下字段信息并以纯JSON格式返回。

字段说明（只提取图片中实际存在的字段，没有的设为空字符串""）：
- recordDate: 日期，统一转为 YYYY-MM-DD 格式（如 2026-07-21）
- shift: 班次（白班 或 夜班）
- factory: 厂房/车间（如 A、A3、B、C）
- serialNumber: 序号/编号/故障维修序号
- machineNo: 机台号/设备编号
- machineModel: 机型/设备出厂编号（如 FANUC、西门子等）
- diagnostician: 诊断人姓名
- repairPerson: 维修人姓名
- confirmer: 确认人姓名
- repairRequestTime: 故障时间/报修时间，统一转为 HH:mm 格式（如 15:30，无分钟则补:00）
- startTime: 开始维修时间/接单时间，格式同上
- endTime: 维修结束时间/诊断结束时间，格式同上
- faultPhenomenon: 故障现象描述
- faultDescription: 维修描述/维修方案/故障原因分析及维修方案
- materialCode: 物料编码/料号/配件编码
- partName: 零件名称/配件名称/使用的配件
- quantity: 数量（纯数字，如 1、2、3）
- machineOnMaterial: 上机物料号（装上的配件编码，标记"装"或"上机"）
- machineOffMaterial: 下机物料号（拆下的配件编码，标记"拆"或"下机"）
- remark: 备注信息
- deliveryRecordRef: 送货记录引用号

重要规则：
1. 只提取图片中实际存在的字段，没有的设为空字符串""，严禁编造
2. 手写文字要仔细辨认，结合上下文推断，但不确定的就留空
3. 日期务必统一为 YYYY-MM-DD 格式（月份和日期补零）
4. 时间务必统一为 HH:mm 格式，如遇到"20时00分"转为"20:00"，"20时"转为"20:00"
5. 中文姓名只取2-3个字的人名，不要带职务或括号
6. 配件编码要完整，包括前缀和数字（如 26T3-0467、J524111330）
7. 返回纯JSON对象，不要用markdown代码块包裹，不要加任何解释文字
8. 如果图片中有"装：XXX"和"拆：YYY"，分别填入上机物料号和下机物料号"""

def call_ocr(image_path, prompt):
    with open(image_path, 'rb') as f:
        img_b64 = base64.b64encode(f.read()).decode()

    payload = json.dumps({
        'model': MODEL,
        'messages': [{
            'role': 'user',
            'content': [
                {'type': 'image_url', 'image_url': {'url': f'data:image/jpeg;base64,{img_b64}'}},
                {'type': 'text', 'text': prompt}
            ]
        }]
    }).encode('utf-8')

    req = urllib.request.Request(API_URL, data=payload, headers={
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {API_KEY}'
    })

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read().decode('utf-8'))
            content = result['choices'][0]['message']['content']
            return content
    except Exception as e:
        return f"ERROR: {e}"

images = sorted(glob.glob(os.path.join(TEST_DIR, "*.jpg")))
print(f"找到 {len(images)} 张测试图片\n")

for i, img in enumerate(images):
    fname = os.path.basename(img)
    fsize = os.path.getsize(img)
    print(f"{'='*60}")
    print(f"图片 {i+1}/{len(images)}: {fname} ({fsize//1024}KB)")

    # 先用简单 prompt 看原始识别
    text = call_ocr(img, SIMPLE_PROMPT)
    print(f"--- 原始识别（自由文本）---")
    # 截断过长输出
    if len(text) > 800:
        text = text[:800] + f"\n... (截断，总长 {len(text)} 字符)"
    print(text)
    print()

    # 再用结构化 prompt
    json_text = call_ocr(img, STRUCTURED_PROMPT)
    print(f"--- 结构化提取 ---")
    if len(json_text) > 800:
        json_text = json_text[:800] + f"\n... (截断，总长 {len(json_text)} 字符)"
    print(json_text)
    print()
