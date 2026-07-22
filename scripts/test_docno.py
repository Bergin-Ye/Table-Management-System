import json, urllib.request

BASE = "http://localhost:8080/api"

# Login
req = urllib.request.Request(f"{BASE}/auth/login",
    data=json.dumps({"username":"admin","password":"admin123"}).encode(),
    headers={"Content-Type":"application/json"})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp["data"]["token"]
headers = {"Content-Type":"application/json", "Authorization": f"Bearer {token}"}

# Create record with documentNo
data = json.dumps({"recordDate":"2026-07-22","shift":"白班","factory":"A","documentNo":"DOC-001","remark":"test"}).encode()
req = urllib.request.Request(f"{BASE}/original-record", data=data, headers=headers)
resp = json.loads(urllib.request.urlopen(req).read())
d = resp["data"]
print(f"Created: id={d['id']}, documentNo={d.get('documentNo')}")

# Verify DB
import subprocess
result = subprocess.run(
    ["mysql", "-u", "root", "-phaoyu2026", "metal_system", "-e",
     f"SELECT id, document_no FROM original_record WHERE id={d['id']}"],
    capture_output=True, text=True)
print(result.stdout)
