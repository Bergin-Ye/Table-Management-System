import json, urllib.request, os

BASE = "http://localhost:8080/api"
TEMPLATE = "E:/code/System/送货记录模板.xlsx"

# Login
req = urllib.request.Request(f"{BASE}/auth/login",
    data=json.dumps({"username":"admin","password":"admin123"}).encode(),
    headers={"Content-Type":"application/json"})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp["data"]["token"]

# Import
with open(TEMPLATE, 'rb') as f:
    file_data = f.read()

boundary = "----TestBoundary"
body = b''
body += f'--{boundary}\r\n'.encode()
body += b'Content-Disposition: form-data; name="file"; filename="delivery_template.xlsx"\r\n'
body += b'Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n'
body += file_data
body += f'\r\n--{boundary}\r\n'.encode()
body += b'Content-Disposition: form-data; name="companyId"\r\n\r\n'
body += b'1'
body += f'\r\n--{boundary}--\r\n'.encode()

req = urllib.request.Request(f"{BASE}/delivery-record/import",
    data=body, headers={
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Authorization": f"Bearer {token}"
    })

try:
    with urllib.request.urlopen(req, timeout=120) as resp:
        result = json.loads(resp.read().decode('utf-8'))
        print(f"Total: {result['data']['total']}")
        print(f"Success: {result['data']['success']}")
        print(f"Fail: {result['data']['fail']}")
        print()
        details = result['data'].get('failDetails', [])
        if details:
            print(f"=== Failed rows ({len(details)}) ===")
            for d in details[:30]:  # show first 30
                print(f"  Row {d.get('row','?')}: {d.get('reason','?')}")
        else:
            print("No failure details returned — need to check backend logging")
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}: {e.read().decode()}")
except Exception as e:
    print(f"Error: {e}")
