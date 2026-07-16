#!/usr/bin/env python3
"""
Metal Factory Excel to MySQL Migration Script
Reads each sheet by index, cleans data, inserts into MySQL
Usage: py -3 scripts/migrate.py [--dry-run]
"""
import pandas as pd
import pymysql
import sys, os

EXCEL = r"e:\code\System\金属厂--2607 .xlsx"
DB = {'host': 'localhost', 'user': 'root', 'password': 'haoyu2026', 'database': 'metal_system', 'charset': 'utf8mb4'}

DRY_RUN = '--dry-run' in sys.argv

def get_conn():
    return pymysql.connect(**DB)

def safe_str(v): return str(v).strip() if pd.notna(v) and v != '' else None
def safe_int(v):
    try: return int(float(str(v))) if pd.notna(v) and v != '' else None
    except: return None
def safe_float(v):
    try: return float(str(v)) if pd.notna(v) and v != '' else None
    except: return None
def safe_date(v):
    """安全转换日期：支持字符串日期和 Excel 日期序列号"""
    try:
        if pd.isna(v) or str(v).strip() == '':
            return None
        try:
            return pd.to_datetime(v).strftime('%Y-%m-%d')
        except Exception:
            # 可能是 Excel 日期序列号（如 "46208.0"），用 origin='1899-12-30' 转换
            serial = float(str(v))
            if serial > 1:
                base = pd.Timestamp('1899-12-30')
                return (base + pd.Timedelta(days=serial)).strftime('%Y-%m-%d')
            return None
    except Exception:
        return None

def safe_datetime(v):
    """安全转换日期时间：支持字符串日期时间和 Excel 日期序列号"""
    try:
        if pd.isna(v) or str(v).strip() == '':
            return None
        try:
            return pd.to_datetime(v).strftime('%Y-%m-%d %H:%M:%S')
        except Exception:
            # 可能是 Excel 日期序列号，转换后只返回日期部分
            serial = float(str(v))
            if serial > 1:
                base = pd.Timestamp('1899-12-30')
                return (base + pd.Timedelta(days=serial)).strftime('%Y-%m-%d')
            return None
    except Exception:
        return None

def clean_col(name):
    if pd.isna(name): return None
    return str(name).strip().replace('\n','').replace('\r','').replace(' ','')

def exec_sql(sql, params=None):
    conn = get_conn()
    cur = conn.cursor()
    try:
        cur.execute(sql, params)
        conn.commit()
    finally:
        cur.close(); conn.close()

def insert_many(table, columns, rows):
    if not rows: return 0
    conn = get_conn()
    cur = conn.cursor()
    placeholders = ','.join(['%s'] * len(columns))
    sql = f"INSERT INTO `{table}` ({','.join([f'`{c}`' for c in columns])}) VALUES ({placeholders})"
    try:
        cur.executemany(sql, rows)
        conn.commit()
        return cur.rowcount
    finally:
        cur.close(); conn.close()

def truncate(tables):
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("SET FOREIGN_KEY_CHECKS = 0")
    for t in tables:
        try: cur.execute(f"TRUNCATE TABLE `{t}`")
        except: pass
    cur.execute("SET FOREIGN_KEY_CHECKS = 1")
    conn.commit()
    cur.close(); conn.close()

# ============================================================
# Data import functions
# ============================================================

def import_delivery():
    """Sheet 0: 1.送货记录"""
    print("[1/6] Delivery Records...")
    df = pd.read_excel(EXCEL, sheet_name=0, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    rows = []
    for _, r in df.iterrows():
        row = (
            safe_date(r.iloc[0]),   # record_date
            safe_str(r.iloc[1]),    # category
            safe_str(r.iloc[2]),    # material_name
            safe_str(r.iloc[3]),    # spec_model
            safe_str(r.iloc[4]),    # material_code
            safe_str(r.iloc[5]),    # material_serial
            safe_int(r.iloc[6]),    # quantity
            safe_str(r.iloc[7]),    # unit
            safe_str(r.iloc[8]),    # brand
            '新品',                 # product_attr (default)
            safe_str(r.iloc[10]),   # factory (skip col 9 = 备注)
            safe_str(r.iloc[11]),   # shipment_no
            None,                   # year_month (calculated by app)
        )
        if row[4]: rows.append(row)  # skip rows without material_code

    cols = ['record_date','category','material_name','spec_model','material_code',
            'material_serial','quantity','unit','brand','product_attr','factory',
            'shipment_no','year_month']
    n = insert_many('delivery_record', cols, rows)
    print(f"  OK: {n} rows")
    return n

def import_original():
    """Sheet 1: 2.7月金属厂原始记录"""
    print("[2/6] Original Records...")
    df = pd.read_excel(EXCEL, sheet_name=1, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    # Excel 列序：年+月, 日期, 班次, 厂房, 序列号, 机台号, 诊断人, 维修人,
    # 报修时间, 开始时间, 结束时间, 维修工时, 停机工时, 机型, 故障现象, (空白),
    # 料号, 配件名称, 数量, 上机物料, 下机物料, 备注, 确认人, 送货记录, (空白),
    # 上次上机时间, 是否过保
    rows = []
    for _, r in df.iterrows():
        vals = r.values
        row = (
            safe_str(vals[0]) if len(vals) > 0 else None,    # 年+月
            safe_date(vals[1]) if len(vals) > 1 else None,   # 日期
            safe_str(vals[2]) if len(vals) > 2 else None,    # 班次
            safe_str(vals[3]) if len(vals) > 3 else None,    # 厂房
            safe_str(vals[4]) if len(vals) > 4 else None,    # 序列号
            safe_str(vals[5]) if len(vals) > 5 else None,    # 机台号
            safe_str(vals[6]) if len(vals) > 6 else None,    # 诊断人
            safe_str(vals[7]) if len(vals) > 7 else None,    # 维修人
            safe_datetime(vals[8]) if len(vals) > 8 else None,   # 报修时间
            safe_datetime(vals[9]) if len(vals) > 9 else None,   # 开始时间
            safe_datetime(vals[10]) if len(vals) > 10 else None, # 结束时间
            safe_float(vals[11]) if len(vals) > 11 else None,    # 维修工时
            safe_float(vals[12]) if len(vals) > 12 else None,    # 停机工时
            safe_str(vals[13]) if len(vals) > 13 else None,  # 机型
            safe_str(vals[14]) if len(vals) > 14 else None,  # 故障现象
            None,                                             # 维修描述（Excel 中无此列）
            safe_str(vals[16]) if len(vals) > 16 else None,  # 料号（跳过 col[15]=空白）
            safe_str(vals[17]) if len(vals) > 17 else None,  # 配件名称
            safe_int(vals[18]) if len(vals) > 18 else None,  # 数量
            safe_str(vals[19]) if len(vals) > 19 else None,  # 上机物料
            safe_str(vals[20]) if len(vals) > 20 else None,  # 下机物料
            safe_str(vals[21]) if len(vals) > 21 else None,  # 备注
            safe_str(vals[22]) if len(vals) > 22 else None,  # 确认人
            safe_str(vals[23]) if len(vals) > 23 else None,  # 送货记录
            safe_date(vals[25]) if len(vals) > 25 else None, # 上次上机时间（跳过 col[24]=空白）
            safe_str(vals[26]) if len(vals) > 26 else None,  # 是否过保
        )
        # 跳过完全空行（Excel 底部无效行）
        if any([row[1], row[5], row[16], row[19], row[20]]):
            rows.append(row)

    cols = ['year_month','record_date','shift','factory','serial_number','machine_no',
            'diagnostician','repair_person','repair_request_time','start_time','end_time',
            'repair_hours','downtime_hours','machine_model','fault_phenomenon',
            'fault_description','material_code','part_name','quantity','machine_on_material',
            'machine_off_material','remark','confirmer','delivery_record_ref',
            'last_machine_on_time','is_out_of_warranty']
    n = insert_many('original_record', cols, rows)
    print(f"  OK: {n} rows")
    return n

def import_machine_material():
    """Sheet 2: 3.7月金属厂上机物料"""
    print("[3/6] Machine Materials...")
    df = pd.read_excel(EXCEL, sheet_name=2, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    # Excel 列序：年+月, 日期, 班次, 厂房, 序列号, 机台号, 维修人,
    # 报修时间, 开始时间, 结束时间, 维修工时, 停机工时, 机型, 故障现象, 维修描述,
    # 料号, 配件名称, 数量, 上机物料, 下机物料, 备注, 确认人, 送货记录,
    # 上次上机时间, 是否过保
    rows = []
    for _, r in df.iterrows():
        vals = r.values
        row = (
            safe_str(vals[0]) if len(vals) > 0 else None,    # 年+月
            safe_date(vals[1]) if len(vals) > 1 else None,   # 日期
            safe_str(vals[2]) if len(vals) > 2 else None,    # 班次
            safe_str(vals[3]) if len(vals) > 3 else None,    # 厂房
            safe_str(vals[4]) if len(vals) > 4 else None,    # 序列号
            safe_str(vals[5]) if len(vals) > 5 else None,    # 机台号
            safe_str(vals[6]) if len(vals) > 6 else None,    # 维修人（此 Sheet 无诊断人列）
            safe_datetime(vals[7]) if len(vals) > 7 else None,   # 报修时间
            safe_datetime(vals[8]) if len(vals) > 8 else None,   # 开始时间
            safe_datetime(vals[9]) if len(vals) > 9 else None,   # 结束时间
            safe_float(vals[10]) if len(vals) > 10 else None,    # 维修工时
            safe_float(vals[11]) if len(vals) > 11 else None,    # 停机工时
            safe_str(vals[12]) if len(vals) > 12 else None,  # 机型
            safe_str(vals[13]) if len(vals) > 13 else None,  # 故障现象
            safe_str(vals[14]) if len(vals) > 14 else None,  # 维修描述
            safe_str(vals[15]) if len(vals) > 15 else None,  # 料号
            safe_str(vals[16]) if len(vals) > 16 else None,  # 配件名称
            safe_int(vals[17]) if len(vals) > 17 else None,  # 数量
            safe_str(vals[18]) if len(vals) > 18 else None,  # 上机物料
            safe_str(vals[19]) if len(vals) > 19 else None,  # 下机物料
            safe_str(vals[20]) if len(vals) > 20 else None,  # 备注
            safe_str(vals[21]) if len(vals) > 21 else None,  # 确认人
            safe_str(vals[22]) if len(vals) > 22 else None,  # 送货记录
            safe_date(vals[23]) if len(vals) > 23 else None, # 上次上机时间
            safe_str(vals[24]) if len(vals) > 24 else None,  # 是否过保
        )
        # 跳过完全空行（Excel 底部无效行）
        if any([row[1], row[5], row[15], row[18], row[19]]):
            rows.append(row)

    cols = ['year_month','record_date','shift','factory','serial_number','machine_no',
            'repair_person','repair_request_time','start_time','end_time',
            'repair_hours','downtime_hours','machine_model','fault_phenomenon',
            'fault_description','material_code','part_name','quantity','machine_on_material',
            'machine_off_material','remark','confirmer','delivery_record_ref',
            'last_machine_on_time','is_out_of_warranty']
    n = insert_many('machine_material', cols, rows)
    print(f"  OK: {n} rows")
    return n

def import_delivery_stats():
    """Sheet 3: 7月送货&超比统计"""
    print("[4/6] Delivery Stats...")
    df = pd.read_excel(EXCEL, sheet_name=3, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    rows = []
    daily_rows = []
    for stat_id, (_, r) in enumerate(df.iterrows(), 1):
        vals = r.values
        row = (
            safe_str(vals[1]) if len(vals) > 1 else None,   # category
            safe_str(vals[2]) if len(vals) > 2 else None,   # material_code
            safe_str(vals[3]) if len(vals) > 3 else None,   # system_name
            safe_str(vals[4]) if len(vals) > 4 else None,   # part_name
            safe_float(vals[5]) if len(vals) > 5 else None, # unit_usage
            safe_float(vals[6]) if len(vals) > 6 else None, # ratio
            safe_float(vals[7]) if len(vals) > 7 else None, # unit_price_with_tax
            safe_int(vals[8]) if len(vals) > 8 else None,   # machine_count
            safe_int(vals[9]) if len(vals) > 9 else None,   # delivery_quantity
            safe_int(vals[10]) if len(vals) > 10 else None, # machine_on_quantity
            safe_int(vals[11]) if len(vals) > 11 else None, # month_repair
            safe_float(vals[12]) if len(vals) > 12 else None, # agreed_ratio_quantity
            safe_float(vals[13]) if len(vals) > 13 else None, # excess_quantity
            safe_float(vals[14]) if len(vals) > 14 else None, # excess_amount_with_tax
            safe_date(vals[15]) if len(vals) > 15 else None,  # stat_date
            None,                                              # year_month
        )
        rows.append(row)

        # Daily columns: positions 16-46 map to day 1-31
        for day_offset in range(31):
            idx = 16 + day_offset
            if idx < len(vals):
                val = safe_float(vals[idx])
                if val is not None:
                    daily_rows.append((stat_id, day_offset + 1, val))

    cols = ['category','material_code','system_name','part_name','unit_usage','ratio',
            'unit_price_with_tax','machine_count','delivery_quantity','machine_on_quantity',
            'month_repair','agreed_ratio_quantity','excess_quantity','excess_amount_with_tax',
            'stat_date','year_month']
    n = insert_many('delivery_stats', cols, rows)
    print(f"  Main: {n} rows")

    dn = insert_many('delivery_stats_daily', ['stat_id','day_number','value'], daily_rows)
    print(f"  Daily: {dn} rows")
    return n

def import_settlement():
    """Sheet 4: 金属厂各机型结算机台数"""
    print("[5/6] Settlement Machines...")
    df = pd.read_excel(EXCEL, sheet_name=4, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    # This sheet: 料号, 类别, 配件名称, 单台机用量, 比例, 价格(含税), 质保期, 价格类型, 备注, 机型, 结算机台数量, ...
    # Skip extra data columns after position 10
    rows = []
    for _, r in df.iterrows():
        vals = r.values
        row = (
            safe_str(vals[0]) if len(vals) > 0 else None,
            safe_str(vals[1]) if len(vals) > 1 else None,
            safe_str(vals[2]) if len(vals) > 2 else None,
            safe_float(vals[3]) if len(vals) > 3 else None,
            safe_float(vals[4]) if len(vals) > 4 else None,
            safe_float(vals[5]) if len(vals) > 5 else None,
            safe_str(vals[6]) if len(vals) > 6 else None,
            safe_str(vals[7]) if len(vals) > 7 else None,
            safe_str(vals[8]) if len(vals) > 8 else None,
            safe_str(vals[9]) if len(vals) > 9 else None,
            safe_int(vals[10]) if len(vals) > 10 else None,
        )
        rows.append(row)

    cols = ['material_code','category','part_name','unit_usage','ratio','unit_price_with_tax',
            'warranty_period','price_type','remark','machine_model','settlement_machine_count']
    n = insert_many('settlement_machine', cols, rows)
    print(f"  OK: {n} rows")
    return n

def import_machine_detail():
    """Sheet 5: 5月机型明细"""
    print("[6/6] Machine Details...")
    df = pd.read_excel(EXCEL, sheet_name=5, dtype=str)
    df.columns = [clean_col(c) for c in df.columns]
    df = df.loc[:, ~df.columns.duplicated()]

    rows = []
    for _, r in df.iterrows():
        vals = r.values
        row = (
            safe_str(vals[0]) if len(vals) > 0 else None,  # factory
            safe_str(vals[1]) if len(vals) > 1 else None,  # machine_no
            safe_str(vals[2]) if len(vals) > 2 else None,  # machine_brand
        )
        if row[0] or row[1]:  # skip completely empty rows
            rows.append(row)

    n = insert_many('machine_detail', ['factory','machine_no','machine_brand'], rows)
    print(f"  OK: {n} rows")
    return n

def extract_materials():
    """Extract distinct materials from delivery_record"""
    print("[Extra] Extracting materials...")
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO material (category, material_name, spec_model, material_code)
        SELECT DISTINCT category, material_name, spec_model, material_code
        FROM delivery_record
        WHERE material_code IS NOT NULL AND material_code != ''
    """)
    n = cur.rowcount
    conn.commit()
    cur.close(); conn.close()
    print(f"  OK: {n} materials")
    return n

def verify():
    print("\n=== Verification ===")
    conn = get_conn()
    cur = conn.cursor()
    for t in ['delivery_record','original_record','machine_material','delivery_stats',
              'delivery_stats_daily','settlement_machine','machine_detail','material']:
        cur.execute(f"SELECT COUNT(*) FROM `{t}`")
        print(f"  {t}: {cur.fetchone()[0]}")
    cur.close(); conn.close()

# ============================================================
# Main
# ============================================================
if __name__ == '__main__':
    print("=== Metal Factory Data Migration ===")

    if not os.path.exists(EXCEL):
        print(f"ERROR: Excel not found: {EXCEL}")
        sys.exit(1)

    # Test DB connection
    try:
        conn = get_conn(); conn.close()
        print("Database: OK")
    except Exception as e:
        print(f"Database ERROR: {e}")
        sys.exit(1)

    if DRY_RUN:
        print("DRY RUN - just reading Excel sheet names")
        xl = pd.ExcelFile(EXCEL)
        for i, name in enumerate(xl.sheet_names):
            df = pd.read_excel(EXCEL, sheet_name=i, nrows=2)
            print(f"  Sheet {i}: {name} - {len(df.columns)} cols")
        sys.exit(0)

    # Clear existing data
    truncate(['delivery_stats_daily','delivery_stats','machine_material','original_record',
              'delivery_record','settlement_machine','machine_detail','material'])
    print("Cleared existing data")

    # Run imports
    import_delivery()
    import_original()
    import_machine_material()
    import_delivery_stats()
    import_settlement()
    import_machine_detail()
    extract_materials()
    verify()

    print("\n[DONE] Migration complete!")
