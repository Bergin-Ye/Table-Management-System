import json, urllib.request

BASE = "http://localhost:8080/api"

# Login
req = urllib.request.Request(f"{BASE}/auth/login",
    data=json.dumps({"username":"admin","password":"admin123"}).encode(),
    headers={"Content-Type":"application/json"})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp["data"]["token"]
headers = {"Content-Type":"application/json", "Authorization": f"Bearer {token}"}

# Test 1: Voice Parse Date
print("=== Test 1: Voice Parse Date ===")
req = urllib.request.Request(f"{BASE}/voice-parse",
    data=json.dumps({"text":"日期2026年7月21日 班次白班 维修人张三","table":"original-record"}).encode(),
    headers=headers)
resp = json.loads(urllib.request.urlopen(req).read())
print(json.dumps(resp, indent=2, ensure_ascii=False))

# Test 2: Delivery Stats Batch Refresh (company 1)
print("\n=== Test 2: Batch Refresh (companyId=1) ===")
req = urllib.request.Request(f"{BASE}/delivery-stats/batch-refresh",
    data=json.dumps({"yearMonth":"2026-07","statMonth":"2026-07","companyId":1}).encode(),
    headers=headers)
resp = json.loads(urllib.request.urlopen(req).read())
print(json.dumps(resp, indent=2, ensure_ascii=False))

# Test 3: 156 cross-company uniqueness
print("\n=== Test 3: 156 cross-company (companyId=2) ===")
req = urllib.request.Request(f"{BASE}/base-material-156",
    data=json.dumps({"materialCode":"OCR-TEST-001","systemName":"CNC","partName":"test","category":"test","unitUsage":1,"ratio":0.15,"unitPriceWithTax":100,"companyId":2}).encode(),
    headers=headers)
try:
    resp = json.loads(urllib.request.urlopen(req).read())
    print(json.dumps(resp, indent=2, ensure_ascii=False))
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}: {e.read().decode()}")

# Test 4: Delivery Stats create with companyId
print("\n=== Test 4: Delivery Stats create with companyId ===")
req = urllib.request.Request(f"{BASE}/delivery-stats",
    data=json.dumps({
        "materialCode":"OCR-TEST-001",
        "systemName":"CNC",
        "category":"test",
        "partName":"test",
        "statDate":"2026-07-21",
        "unitUsage":1,
        "ratio":0.15,
        "unitPriceWithTax":100,
        "companyId":1
    }).encode(),
    headers=headers)
try:
    resp = json.loads(urllib.request.urlopen(req).read())
    print(json.dumps(resp, indent=2, ensure_ascii=False))
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}: {e.read().decode()}")
