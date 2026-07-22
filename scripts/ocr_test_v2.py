import json, urllib.request, os

# 用本地 API 测试（走完整的提取流程）
BASE = "http://localhost:8080/api"

# Login
req = urllib.request.Request(f"{BASE}/auth/login",
    data=json.dumps({"username":"admin","password":"admin123"}).encode(),
    headers={"Content-Type":"application/json"})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp["data"]["token"]

TEST_DIR = "E:/code/System/测试图片"
images = sorted(os.listdir(TEST_DIR))[:2]  # 只测2张

for fname in images:
    img_path = os.path.join(TEST_DIR, fname)
    fsize = os.path.getsize(img_path)

    with open(img_path, 'rb') as f:
        img_data = f.read()

    boundary = "----FormBoundary7MA4YWxkTrZu0gW"
    body = b''
    body += f'--{boundary}\r\n'.encode()
    body += f'Content-Disposition: form-data; name="image"; filename="{fname}"\r\n'.encode()
    body += f'Content-Type: image/jpeg\r\n\r\n'.encode()
    body += img_data
    body += f'\r\n--{boundary}\r\n'.encode()
    body += f'Content-Disposition: form-data; name="tableType"\r\n\r\n'.encode()
    body += b'original-record'
    body += f'\r\n--{boundary}--\r\n'.encode()

    req = urllib.request.Request(f"{BASE}/ocr/recognize",
        data=body, headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Authorization": f"Bearer {token}"
        })

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            r = json.loads(resp.read().decode('utf-8'))
            data = r.get('data', {})
            fields = data.get('fields', {})
            count = data.get('filledCount', 0)
            raw = data.get('rawText', '')

            print(f"{'='*60}")
            print(f"图片: {fname} ({fsize//1024}KB)")
            print(f"识别字段数: {count}")
            print(f"字段: {json.dumps(fields, ensure_ascii=False, indent=2)}")
            if data.get('error'):
                print(f"错误: {data['error']}")
            print(f"原文(前200字): {raw[:200]}...")
    except Exception as e:
        print(f"{fname}: ERROR - {e}")
    print()
