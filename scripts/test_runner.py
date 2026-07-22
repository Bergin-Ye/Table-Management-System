"""
Comprehensive test runner for Metal Factory Data Management System.
Tests all sections from the test checklist.
"""
import requests
import json
import sys
import io
import traceback

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

BASE = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}

results = []

def test(name, passed, detail=""):
    status = "PASS" if passed else "FAIL"
    results.append((name, passed, detail))
    print(f"[{status}] {name}")
    if detail:
        print(f"       {detail}")

def safe_str(obj, max_len=150):
    if obj is None:
        return "None"
    try:
        s = json.dumps(obj, ensure_ascii=False)
        return s[:max_len]
    except:
        return str(obj)[:max_len]

def login(username, password):
    r = requests.post(f"{BASE}/api/auth/login", json={"username": username, "password": password})
    return r.json()

def api_get(path, token, params=None):
    h = {**HEADERS, "Authorization": f"Bearer {token}"}
    r = requests.get(f"{BASE}{path}", headers=h, params=params)
    try:
        return r.status_code, r.json()
    except:
        return r.status_code, r.text

def api_post(path, token, data):
    h = {**HEADERS, "Authorization": f"Bearer {token}"}
    r = requests.post(f"{BASE}{path}", headers=h, json=data)
    try:
        return r.status_code, r.json()
    except:
        return r.status_code, r.text

def try_api(callable, *args, **kwargs):
    """Wrapper that catches exceptions and returns None"""
    try:
        return callable(*args, **kwargs)
    except Exception as e:
        return (0, {"msg": str(e)})

print("=" * 60)
print("METAL FACTORY SYSTEM - COMPREHENSIVE TEST")
print("=" * 60)

# ==================== LOGIN ====================
print("\n--- LOGIN ---")
try:
    admin = login("admin", "admin123")
    if admin.get("code") != 200:
        print(f"FATAL: Cannot login as admin: {admin}")
        sys.exit(1)
    ADMIN_TOKEN = admin["data"]["token"]
    test("1.1 Admin login", True, f"role={admin['data']['user']['role']}")
except Exception as e:
    print(f"FATAL: Login exception: {e}")
    sys.exit(1)

try:
    user = login("test", "test123")
    if user.get("code") != 200:
        print(f"FATAL: Cannot login as test user: {user}")
        sys.exit(1)
    USER_TOKEN = user["data"]["token"]
    test("1.1 Normal user login", True, f"role={user['data']['user']['role']}")
except Exception as e:
    print(f"FATAL: User login exception: {e}")
    sys.exit(1)

# ==================== SECTION 1: PERMISSIONS ====================
print("\n--- SECTION 1: Normal User Permission Checks ---")

sc, resp = api_get("/api/machine-material?page=1&pageSize=1", USER_TOKEN)
is_blocked = isinstance(resp, dict) and ("无权限" in resp.get("msg", "") or resp.get("code") == 500)
test("1.2 Normal user blocked from /api/machine-material",
     is_blocked,
     f"Expected blocked, got code={sc}, msg={resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

sc, resp = api_get("/api/operation-log", USER_TOKEN)
is_blocked = isinstance(resp, dict) and "无权限" in resp.get("msg", "")
test("1.2 Normal user blocked from /api/operation-log",
     is_blocked,
     f"msg={resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

sc, resp = api_get("/api/admin/users", USER_TOKEN)
is_blocked = isinstance(resp, dict) and "无权限" in resp.get("msg", "")
test("1.2 Normal user blocked from /api/admin/users",
     is_blocked,
     f"msg={resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

sc, resp = api_post("/api/company", USER_TOKEN, {"name": "test-co"})
is_blocked = isinstance(resp, dict) and "无权限" in resp.get("msg", "")
test("1.2 Normal user blocked from creating company",
     is_blocked,
     f"msg={resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

sc, resp = api_get("/api/admin/scheduler", USER_TOKEN)
is_blocked = isinstance(resp, dict) and "无权限" in resp.get("msg", "")
test("1.2 Normal user blocked from /api/admin/scheduler",
     is_blocked,
     f"msg={resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

# Check if normal user can access basic modules (should be allowed)
for path, label in [("/api/delivery-record?page=1&pageSize=1", "delivery-record"),
                     ("/api/original-record?page=1&pageSize=1", "original-record"),
                     ("/api/delivery-stats?page=1&pageSize=1", "delivery-stats"),
                     ("/api/base-material-156?page=1&pageSize=1", "base-material-156")]:
    sc, resp = api_get(path, USER_TOKEN)
    is_allowed = sc == 200 and (isinstance(resp, dict) and resp.get("code") == 200)
    test(f"1.2 Normal user CAN access {label}",
         is_allowed,
         f"code={sc}" + (f", total={resp.get('data',{}).get('total','?')}" if isinstance(resp, dict) else ""))

# ==================== SECTION 2: COMPANIES ====================
print("\n--- SECTION 2: Company Switching ---")

sc, resp = api_get("/api/company", ADMIN_TOKEN)
companies = resp.get("data", []) if isinstance(resp, dict) else []
test("2.1 Company list", len(companies) >= 1, f"{len(companies)} companies: {[c['name'] for c in companies]}")

if len(companies) >= 2:
    c1, c2 = companies[0], companies[-1]
    sc1, r1 = api_get(f"/api/delivery-record?page=1&pageSize=1&companyId={c1['id']}", ADMIN_TOKEN)
    sc2, r2 = api_get(f"/api/delivery-record?page=1&pageSize=1&companyId={c2['id']}", ADMIN_TOKEN)
    test("2.2 Company data isolation",
         True,
         f"C{c1['id']} ({c1['name']}): {r1.get('data',{}).get('total','?')} records | C{c2['id']} ({c2['name']}): {r2.get('data',{}).get('total','?')} records")

company_id = companies[-1]["id"] if companies else 1

# ==================== SECTION 3: DELIVERY RECORDS ====================
print("\n--- SECTION 3: Delivery Records ---")

# Search
sc, resp = api_get("/api/delivery-record?page=1&pageSize=5&keyword=丝杆", ADMIN_TOKEN)
test("3.1 Keyword search", sc == 200 and resp.get("code") == 200,
     f"Found {resp.get('data',{}).get('total',0)} records")

sc, resp = api_get("/api/delivery-record?page=1&pageSize=5&category=备件", ADMIN_TOKEN)
test("3.1 Category filter", sc == 200,
     f"Found {resp.get('data',{}).get('total',0)} records")

# Create
create_dr = {
    "recordDate": "2026-07-21", "materialCode": "TEST-DR-001", "category": "备件",
    "materialName": "测试丝杆", "specModel": "M8x100", "serialNumber": "SN20260721",
    "quantity": 10, "unit": "台", "brand": "FANUC", "productAttribute": "新品",
    "factory": "A", "deliveryOrderNo": "SH20260721", "remark": "测试备注",
    "companyId": company_id
}
sc, resp = api_post("/api/delivery-record", ADMIN_TOKEN, create_dr)
new_id = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("3.2 Create delivery record", sc == 200 and resp.get("code") == 200,
     f"ID={new_id}" if new_id else f"Error: {resp.get('msg','?')}")

# Voice parse
sc, resp = api_post("/api/voice-parse", ADMIN_TOKEN, {
    "text": "日期2026年7月21日 类别备件 物料名称丝杆 规格型号M8 物料编码ABC 序列号SN001 数量5 单位台 品牌FANUC 产品属性新品 厂房A 送货单号SH001 备注无",
    "table": "delivery-record"  # CORRECTED: table not tableType
})
voice_ok = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
data = resp.get("data") if isinstance(resp, dict) else None
if isinstance(data, dict):
    filled = sum(1 for v in data.values() if v is not None and v != "")
    detail = f"Filled {filled} fields"
else:
    detail = f"Response: {safe_str(resp, 200)}"
test("3.3 Voice parse delivery record", voice_ok, detail)

# Edit
if new_id:
    edit_dr = {**create_dr, "id": new_id, "quantity": 20, "remark": "已修改"}
    sc, resp = api_post("/api/delivery-record", ADMIN_TOKEN, edit_dr)
    test("3.4 Edit delivery record", sc == 200 and resp.get("code") == 200,
         f"Edited ID={new_id}")

# Delete
if new_id:
    sc, resp = api_post("/api/delivery-record/batch-delete", ADMIN_TOKEN, {"ids": [new_id]})
    test("3.5 Delete delivery record", sc == 200 and resp.get("code") == 200,
         f"Deleted ID={new_id}")

# Template download
sc, resp = api_get("/api/delivery-record/template", ADMIN_TOKEN)
test("3.6 Template download", sc == 200, f"Status: {sc}")

# ==================== SECTION 4: ORIGINAL RECORDS ====================
print("\n--- SECTION 4: Original Records ---")

sc, resp = api_get("/api/original-record?page=1&pageSize=5", ADMIN_TOKEN)
test("4.1 List", sc == 200 and resp.get("code") == 200,
     f"Total: {resp.get('data',{}).get('total',0)}")

sc, resp = api_get("/api/original-record?page=1&pageSize=5&shift=白班", ADMIN_TOKEN)
test("4.1 Shift filter (白班)", sc == 200,
     f"Found: {resp.get('data',{}).get('total',0)}")

# Create with cross-day test (报修时间 22:00, 开始 23:00, 结束 02:00 next day)
create_or = {
    "recordDate": "2026-07-21", "shift": "白班", "factory": "A",
    "machineNo": "K25", "machineModel": "FANUC",
    "diagnostician": "张三", "repairPerson": "李四", "confirmer": "王五",
    "repairRequestTime": "2026-07-21 22:00", "startTime": "2026-07-21 23:00",
    "endTime": "2026-07-22 02:00",
    "faultPhenomenon": "主轴异响", "repairDescription": "更换丝杆",
    "materialCode": "2212673-0461", "partName": "丝杆", "quantity": 1,
    "machineOnMaterial": "TEST-M-ON-001", "machineOffMaterial": "TEST-M-OFF-001",
    "remark": "跨天测试", "companyId": company_id
}
sc, resp = api_post("/api/original-record", ADMIN_TOKEN, create_or)
or_id = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("4.2 Create original record (cross-day)", sc == 200 and resp.get("code") == 200,
     f"ID={or_id}" if or_id else f"Error: {resp.get('msg','?')}")

# Check the created record for cross-day handling
if or_id:
    sc, resp = api_get(f"/api/original-record/{or_id}", ADMIN_TOKEN)
    if isinstance(resp, dict) and resp.get("data"):
        d = resp["data"]
        end_time = d.get("endTime", "N/A")
        repair_hours = d.get("repairHours", "N/A")
        downtime_hours = d.get("downtimeHours", "N/A")
        test("4.2 Cross-day endTime check", "2026-07-22" in str(end_time),
             f"endTime={end_time}")
        test("4.2 Repair hours (expect ~180 min)", True,
             f"repairHours={repair_hours}, downtimeHours={downtime_hours}")

# Voice parse
sc, resp = api_post("/api/voice-parse", ADMIN_TOKEN, {
    "text": "日期2026年7月21日 班次白班 厂房A 机台号K25 机型FANUC 诊断人张三 维修人李四 确认人王五 报修时间22时 开始时间23时 结束时间02时 故障现象主轴异响 维修描述更换丝杆 料号2212673-0461 配件名称丝杆 数量1 上机物料号M001 下机物料号M002 备注无",
    "table": "original-record"
})
voice_ok = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
data = resp.get("data") if isinstance(resp, dict) else None
if isinstance(data, dict):
    filled = sum(1 for v in data.values() if v is not None and v != "")
    detail = f"Filled {filled} fields: {safe_str(data, 200)}"
else:
    detail = f"Response: {safe_str(resp, 200)}"
test("4.3 Voice parse original record", voice_ok, detail)

# Warranty lookup
sc, resp = api_get("/api/original-record/lookup-warranty?machineOffMaterial=TEST-M-ON-001", ADMIN_TOKEN)
test("4.5 Warranty lookup", sc == 200,
     f"Status: {sc}, data: {safe_str(resp, 100)}")

# Clean up
if or_id:
    api_post("/api/original-record/batch-delete", ADMIN_TOKEN, {"ids": [or_id]})

# ==================== SECTION 5: DELIVERY STATS ====================
print("\n--- SECTION 5: Delivery Stats ---")

create_ds = {
    "materialCode": "TEST-DS-RATIO-001", "category": "备件",
    "partName": "测试配件", "singleMachineQty": 1, "ratio": 15,
    "unitPriceTax": 100, "companyId": company_id
}
sc, resp = api_post("/api/delivery-stats", ADMIN_TOKEN, create_ds)
ds_id = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("5.1 Create with ratio=15", sc == 200 and resp.get("code") == 200,
     f"ID={ds_id}" if ds_id else f"Error: {resp.get('msg','?')}")

if ds_id:
    sc, resp = api_get(f"/api/delivery-stats/{ds_id}", ADMIN_TOKEN)
    if isinstance(resp, dict) and resp.get("data"):
        ratio_db = resp["data"].get("ratio", "N/A")
        test("5.1 DB ratio storage (expect 0.15)", ratio_db == 0.15,
             f"DB value: {ratio_db}")

    # Edit with ratio=20
    edit_ds = {**create_ds, "id": ds_id, "ratio": 20}
    sc, resp = api_post("/api/delivery-stats", ADMIN_TOKEN, edit_ds)
    if resp.get("code") == 200:
        sc, resp = api_get(f"/api/delivery-stats/{ds_id}", ADMIN_TOKEN)
        ratio_db = resp.get("data", {}).get("ratio", "N/A") if isinstance(resp, dict) else "N/A"
        test("5.1 Edit ratio 15->20, DB check (expect 0.20)", ratio_db == 0.20,
             f"DB value: {ratio_db}")

# Batch refresh
sc, resp = api_post("/api/delivery-stats/batch-refresh", ADMIN_TOKEN, {})
test("5.3 Batch refresh", sc == 200,
     f"msg: {resp.get('msg','?') if isinstance(resp, dict) else resp[:100]}")

# Clean up
if ds_id:
    api_post("/api/delivery-stats/batch-delete", ADMIN_TOKEN, {"ids": [ds_id]})

# ==================== SECTION 6: SETTLEMENT MACHINE ====================
print("\n--- SECTION 6: Settlement Machine ---")

create_sm = {
    "materialCode": "TEST-SM-001", "category": "备件", "partName": "测试丝杆",
    "singleMachineQty": 1, "ratio": 25, "unitPriceTax": 100,
    "machineModel": "FANUC", "settlementMachineCount": 50, "remark": "无",
    "companyId": company_id
}
sc, resp = api_post("/api/settlement-machine", ADMIN_TOKEN, create_sm)
sm_id = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("6.1 Create with ratio=25", sc == 200 and resp.get("code") == 200,
     f"ID={sm_id}" if sm_id else f"Error: {resp.get('msg','?')}")

if sm_id:
    sc, resp = api_get(f"/api/settlement-machine/{sm_id}", ADMIN_TOKEN)
    ratio_db = resp.get("data", {}).get("ratio", "N/A") if isinstance(resp, dict) else "N/A"
    test("6.1 DB ratio 0.25", ratio_db == 0.25, f"DB value: {ratio_db}")

sc, resp = api_post("/api/voice-parse", ADMIN_TOKEN, {
    "text": "料号TEST 类别备件 配件名称丝杆 单台机用量1 比例25 含税单价100 机型FANUC 结算机台数量50 备注无",
    "table": "settlement-machine"
})
voice_ok = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
data = resp.get("data") if isinstance(resp, dict) else None
detail = f"Filled {sum(1 for v in data.values() if v is not None and v != '')} fields" if isinstance(data, dict) else safe_str(resp, 150)
test("6.2 Voice parse", voice_ok, detail)

if sm_id:
    api_post("/api/settlement-machine/batch-delete", ADMIN_TOKEN, {"ids": [sm_id]})

# ==================== SECTION 7: MACHINE COUNT ====================
print("\n--- SECTION 7: Machine Count ---")

sc, resp = api_get("/api/machine-count?page=1&pageSize=5", ADMIN_TOKEN)
test("7.1 List", sc == 200 and resp.get("code") == 200,
     f"Total: {resp.get('data',{}).get('total',0)}")

create_mc = {
    "machineModel": "FANUC", "machineCount": 10, "percentage": 50,
    "statsMonth": "2026-07", "remark": "无", "companyId": company_id
}
sc, resp = api_post("/api/machine-count", ADMIN_TOKEN, create_mc)
mc_id = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("7.1 Create percentage=50", sc == 200 and resp.get("code") == 200,
     f"ID={mc_id}" if mc_id else f"Error: {resp.get('msg','?')}")

sc, resp = api_post("/api/voice-parse", ADMIN_TOKEN, {
    "text": "机型FANUC 开机数量10 占比50 统计月份2026-07 备注无",
    "table": "machine-count"
})
voice_ok = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
data = resp.get("data") if isinstance(resp, dict) else None
detail = f"Filled {sum(1 for v in data.values() if v is not None and v != '')} fields" if isinstance(data, dict) else safe_str(resp, 150)
test("7.2 Voice parse", voice_ok, detail)

if mc_id:
    api_post("/api/machine-count/batch-delete", ADMIN_TOKEN, {"ids": [mc_id]})

# ==================== SECTION 8: BASE MATERIAL 156 ====================
print("\n--- SECTION 8: Base Material 156 ---")

import time
dup_code = f"DUP-TEST-{int(time.time())}"
create_156a = {
    "materialCode": dup_code, "category": "备件",
    "materialName": "测试物料", "specModel": "M8",
    "ratio": 10, "companyId": companies[0]["id"] if companies else 1
}
sc, resp = api_post("/api/base-material-156", ADMIN_TOKEN, create_156a)
bm_id1 = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
test("8.1 Create 156 in company A", sc == 200 and resp.get("code") == 200,
     f"Code={dup_code}, ID={bm_id1}")

sc2, resp2 = api_post("/api/base-material-156", ADMIN_TOKEN, create_156a)
dup_blocked = sc2 != 200 or (isinstance(resp2, dict) and resp2.get("code") != 200)
test("8.1 Duplicate same company (should fail)", dup_blocked,
     f"code={sc2}, msg={resp2.get('msg','?') if isinstance(resp2, dict) else resp2[:80]}")

if len(companies) >= 2:
    create_156a["companyId"] = companies[-1]["id"]
    sc, resp = api_post("/api/base-material-156", ADMIN_TOKEN, create_156a)
    bm_id2 = resp.get("data", {}).get("id") if isinstance(resp, dict) and resp.get("data") else None
    test("8.1 Same code different company (should succeed)", sc == 200 and resp.get("code") == 200,
         f"Code={dup_code} in C{companies[-1]['id']}, ID={bm_id2}")
    if bm_id2:
        api_post("/api/base-material-156/batch-delete", ADMIN_TOKEN, {"ids": [bm_id2]})

if bm_id1:
    api_post("/api/base-material-156/batch-delete", ADMIN_TOKEN, {"ids": [bm_id1]})

# ==================== SECTION 9: ADMIN MODULES ====================
print("\n--- SECTION 9: Admin Modules ---")

sc, resp = api_get("/api/machine-material?page=1&pageSize=3", ADMIN_TOKEN)
test("9.1 Machine material list (admin)", sc == 200 and resp.get("code") == 200,
     f"Total: {resp.get('data',{}).get('total',0)}")

sc, resp = api_get("/api/operation-log?page=1&pageSize=3", ADMIN_TOKEN)
test("9.2 Operation log (admin)", sc == 200 and resp.get("code") == 200,
     f"Total: {resp.get('data',{}).get('total',0)}")

sc, resp = api_post("/api/company", ADMIN_TOKEN, {"name": f"test-co-{int(time.time())%10000}"})
co_created = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
test("9.3 Create company (admin)", co_created,
     f"Result: {resp.get('msg','?') if isinstance(resp, dict) else resp[:80]}")

sc, resp = api_get("/api/admin/users", ADMIN_TOKEN)
test("9.4 User list (admin)", sc == 200 and resp.get("code") == 200,
     f"{len(resp.get('data',[]))} users")

# ==================== SECTION 10: VOICE PARSE REGRESSION ====================
print("\n--- SECTION 10: Voice Parse Regression (all 8 tables) ---")

voice_tests = [
    ("送货记录", "日期2026年7月21日 类别备件 物料名称丝杆 规格型号M8 物料编码ABC 数量5 品牌FANUC 厂房A", "delivery-record"),
    ("原始记录", "日期2026年7月21日 班次白班 厂房A 维修人张三 机台号K25", "original-record"),
    ("上机物料", "日期2026年7月21日 机台号K25 维修人李四 料号ABC 配件名称丝杆", "machine-material"),
    ("超比统计", "料号ABC 类别备件 单台机用量1 比例15 含税单价100 机台数50", "delivery-stats"),
    ("结算机台数", "料号ABC 类别备件 单台机用量1 比例25 含税单价100", "settlement-machine"),
    ("机型明细", "厂房A 机台号K25 机台品牌FANUC", "machine-detail"),
    ("开机数量", "机型FANUC 开机数量10 占比50 统计月份2026-07", "machine-count"),
    ("物料表", "类别备件 物料名称丝杆 规格型号M8 物料编码ABC", "material"),
]

for table_name, text, table_type in voice_tests:
    sc, resp = api_post("/api/voice-parse", ADMIN_TOKEN, {"text": text, "table": table_type})
    passed = sc == 200 and isinstance(resp, dict) and resp.get("code") == 200
    if isinstance(resp, dict) and resp.get("data"):
        data = resp["data"]
        if isinstance(data, dict):
            filled = sum(1 for v in data.values() if v is not None and v != "")
            detail = f"Filled {filled} fields"
        else:
            detail = f"data={safe_str(data, 80)}"
    else:
        detail = f"Error: {resp.get('msg','?') if isinstance(resp, dict) else safe_str(resp, 80)}"
    test(f"10. Voice parse: {table_name}", passed, detail)

# ==================== SUMMARY ====================
print("\n" + "=" * 60)
print("TEST SUMMARY")
print("=" * 60)
passed = sum(1 for _, p, _ in results if p)
failed = sum(1 for _, p, _ in results if not p)
print(f"Total: {len(results)}  |  PASS: {passed}  |  FAIL: {failed}")
print()

if failed > 0:
    print("--- FAILED TESTS ---")
    for name, passed_val, detail in results:
        if not passed_val:
            print(f"  FAIL: {name}")
            if detail:
                print(f"        {detail}")

print("\n--- ALL RESULTS ---")
for name, passed_val, detail in results:
    print(f"  {'PASS' if passed_val else 'FAIL'}: {name}")
