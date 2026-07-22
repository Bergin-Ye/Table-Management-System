import json, base64, urllib.request, os

API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
API_KEY = "sk-ws-H.EHILRPR.09wY.MEUCIQD2sGZkPAr9nUM1Mz4UAFUfNoXXF12--vepFtDEr9cPQwIgSIcnKvlw7WPLEizRq_UzhVoghF48ykNEGxHOW4OVQa0"
MODEL = "qwen3.5-ocr"
TEST_DIR = "E:/code/System/测试图片"

# 只测文件大小最大的两张（通常内容更多）
images = sorted(
    [f for f in os.listdir(TEST_DIR) if f.endswith('.jpg')],
    key=lambda f: os.path.getsize(os.path.join(TEST_DIR, f)),
    reverse=True
)[:2]

PROMPT = """请仔细观察这张图片，识别图片中所有的文字，只输出你看到的文字内容，逐行列出。不要遗漏任何手写或印刷文字。"""

for fname in images:
    img_path = os.path.join(TEST_DIR, fname)
    fsize = os.path.getsize(img_path)

    with open(img_path, 'rb') as f:
        img_b64 = base64.b64encode(f.read()).decode()

    payload = json.dumps({
        'model': MODEL,
        'messages': [{
            'role': 'user',
            'content': [
                {'type': 'image_url', 'image_url': {'url': f'data:image/jpeg;base64,{img_b64}'}},
                {'type': 'text', 'text': PROMPT}
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
    except Exception as e:
        content = f"ERROR: {e}"

    print(f"{'='*60}")
    print(f"图片: {fname} ({fsize//1024}KB)")
    print(content)
    print()
