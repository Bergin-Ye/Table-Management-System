import json, base64, urllib.request, sys

# Read image and convert to base64
img_path = 'E:/code/System/测试图片/e5f33ba2-5759-4de9-aeec-83ff08403b29(1).jpg'
with open(img_path, 'rb') as f:
    img_b64 = base64.b64encode(f.read()).decode()

# Build request
payload = json.dumps({
    'model': 'qwen3.5-ocr',
    'messages': [{
        'role': 'user',
        'content': [
            {'type': 'image_url', 'image_url': {'url': f'data:image/jpeg;base64,{img_b64}'}},
            {'type': 'text', 'text': '你是一个工厂维修工单OCR助手。请识别图片中的所有文字，只返回纯文本内容，不要加任何解释。'}
        ]
    }]
}).encode('utf-8')

# Make API call
req = urllib.request.Request(
    'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    data=payload,
    headers={
        'Content-Type': 'application/json',
        'Authorization': 'Bearer sk-ws-H.EHILRPR.09wY.MEUCIQD2sGZkPAr9nUM1Mz4UAFUfNoXXF12--vepFtDEr9cPQwIgSIcnKvlw7WPLEizRq_UzhVoghF48ykNEGxHOW4OVQa0'
    }
)

try:
    with urllib.request.urlopen(req, timeout=60) as resp:
        result = json.loads(resp.read().decode('utf-8'))
        print(json.dumps(result, indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Error: {e}")
    if hasattr(e, 'read'):
        print(e.read().decode('utf-8'))
