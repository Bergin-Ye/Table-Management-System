# 156项表 + 自动回填 + 定时任务 — 设计文档

> 日期: 2026-07-18
> 状态: 待实现

---

## 1. 概述

在现有金属厂数据管理系统中新增"156项"基础物料表，并改造原始记录、超比统计、结算机台数三个模块，实现智能自动回填和跨表数据查询。

---

## 2. 数据库变更

### 2.1 新增表 `base_material_156`

```sql
CREATE TABLE `base_material_156` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT COMMENT '公司ID',
    `category` VARCHAR(100) COMMENT '类别',
    `material_code` VARCHAR(100) NOT NULL COMMENT '料号',
    `system_name` VARCHAR(200) COMMENT '系统名称',
    `part_name` VARCHAR(200) COMMENT '配件名称',
    `unit_usage` DECIMAL(10,4) COMMENT '单台机用量',
    `ratio` DECIMAL(10,4) COMMENT '比例',
    `unit_price_with_tax` DECIMAL(12,4) COMMENT '含税单价',
    `created_by` VARCHAR(50),
    `updated_by` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_b156_mcode` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

字段说明：ID、类别、料号（唯一）、系统名称、配件名称、单台机用量、比例、含税单价。

### 2.2 修改表 `settlement_machine`

新增字段：
```sql
ALTER TABLE `settlement_machine` ADD COLUMN `stat_month` VARCHAR(7) COMMENT '统计月份 格式yyyy-MM';
```

### 2.3 修改表 `delivery_stats`

现有 `material_code` 字段标签从前端改为"料号"。数据库字段名不变，仅前端 label 变更。

---

## 3. 后端实现

### 3.1 新增模块：`base_material_156`

| 层 | 文件 | 说明 |
|----|------|------|
| Entity | `BaseMaterial156.java` | 8个业务字段 + EasyExcel 注解 |
| Mapper | `BaseMaterial156Mapper.java` | CRUD + search + batchInsert，料号唯一校验 |
| Service | `BaseMaterial156Service.java` | 完整 CRUD + 导入导出 + 模板下载 |
| Controller | `BaseMaterial156Controller.java` | `/api/base-material-156` |

**关键 API：**
- `GET /api/base-material-156/search?keyword=` — 不分页模糊搜索，供 autocomplete 使用（搜索料号、系统名称、配件名称）

### 3.2 修改模块：`original_record`

**新增 API：**
- `GET /api/original-record/lookup-156?materialCode=` — 根据料号查询156项表，返回配件名称

**现有逻辑不变**，仅在 `OriginalRecordService` 新增一个查询方法。

### 3.3 修改模块：`delivery_stats`

**新增 API：**
- `GET /api/delivery-stats/auto-fill?materialCode=&statDate=` — 综合查询接口，返回：

```json
{
  "from156": {
    "category": "...", "systemName": "...", "partName": "...",
    "unitUsage": 1.5, "ratio": 0.8, "unitPriceWithTax": 120.00
  },
  "machineCount": 100,           // 来自 settlement_machine，按料号+月份查
  "deliveryQuantity": 50,        // delivery_record 当月同料号记录数
  "machineOnQuantity": 30,       // original_record 当月同料号记录数
  "monthRepair": 5,              // original_record 当月同料号+未过保记录数
  "dailyQuantities": [           // 当月每天送货数量
    { "day": 1, "count": 3 },
    { "day": 2, "count": 5 },
    ...
  ]
}
```

**修改 Service 计算逻辑：**
- `excessAmountWithTax` 公式改为：`(unitPriceWithTax × excessQuantity) / 1.13`
- 约定比例数量 = `machineCount（用户手动输入） × unitUsage × ratio`

**`GET /api/delivery-stats/search-by-name?systemName=`** — 根据系统名称模糊查询156项，返回完整字段（用于系统名称输入框自动匹配）

### 3.4 修改模块：`settlement_machine`

**新增 API：**
- `GET /api/settlement-machine/lookup-156?materialCode=` — 根据料号查询156项表，返回类别/配件名称/单台机用量/比例/含税单价
- `GET /api/machine-count/by-month?statMonth=2026-07` — 按月份查询开机数量表，返回机型+开机台数

**Service 修改：**
- `create`/`update` 方法中 `warrantyPeriod` 默认值设为 "6个月"
- 新增 `statMonth` 字段支持

### 3.5 新增定时任务

```java
@Component
public class DeliveryStatsScheduler {
    @Scheduled(cron = "0 0 * * * *")  // 每小时整点执行
    public void refreshCurrentMonthStats() {
        // 1. 查询当前月份所有 delivery_stats 记录
        // 2. 逐条重新查询送货数量、上机数量、返修数量、每日明细
        // 3. 重新计算超比相关字段
        // 4. 批量更新
    }
}
```

配置类启用：`@EnableScheduling`

---

## 4. 前端实现

### 4.1 新增页面：156项 (`/base-material-156`)

**文件：** `frontend/src/views/base-material-156/BaseMaterial156View.vue`

- 完整 CRUD 表格（复用 PageHeader/SearchForm/ToolBar 组件）
- 搜索：关键词（模糊搜索料号/系统名称/配件名称/类别）
- 表单：料号必填 + 唯一性校验（提交时后端校验，重复提示"料号已存在"）
- 导入导出：复用现有模式
- 路由 + API 文件

### 4.2 修改页面：原始记录

- 料号输入框改为 `el-autocomplete`，调用 `/api/base-material-156/search` 模糊搜索
- 选中后调用 `/api/original-record/lookup-156` 获取配件名称并回填
- 查询无数据时保持空值

### 4.3 修改页面：送货超比统计

**表单改造（重点）：**

| 字段 | 改动 |
|------|------|
| 物料编码 → **料号** | 标签重命名；改为 `el-autocomplete`，匹配156项表 |
| 系统名称 | 改为 `el-autocomplete`，匹配156项表 |
| 类别/配件名称/单台机用量/比例/含税单价 | 选中料号或系统名称后 **自动回填**（来自156项） |
| 机台数 | 选中料号后自动从结算机台数查询回填（用户可手动修改） |
| 送货数量 | 选中料号后自动从送货记录查询当月同料号记录数（只读） |
| 上机数量 | 选中料号后自动从原始记录查询当月同料号记录数（只读） |
| 当月返修 | 选中料号后自动从原始记录查询当月同料号+未过保记录数（只读） |
| 约定比例数量 | 自动计算 = 机台数 × 单台机用量 × 比例 |
| 超比数量合计 | 自动计算 = 送货数量 - 返修数量 |
| 超比含税金额合计 | 自动计算 = (含税单价 × 超比数量) / 1.13 |
| 每日明细（1~31日） | 自动查询并只读展示，根据所选日期动态显示当月天数 |

**交互流程：**
1. 用户选择统计日期 → 确定月份
2. 用户在料号输入框输入 → 模糊匹配156项表 → 选中一条
3. 前端调用 `/api/delivery-stats/auto-fill?materialCode=xxx&statDate=2026-07-01`
4. 后端返回所有自动计算字段 → 前端回填
5. 用户手动输入/修改机台数 → 约定比例数量联动更新
6. 用户提交 → 后端最终计算并存储

**通用规则：** 任何自动查询未查到数据时，对应字段保持空值，不做回填。

### 4.4 修改页面：结算机台数

| 改动项 | 说明 |
|--------|------|
| 物料编码 → **料号** | 标签重命名 |
| 系数 → **比例** | 标签重命名 |
| 料号输入框 | 改为 `el-autocomplete`，匹配156项表 |
| 自动回填 | 选中料号后回填：类别、配件名称、单台机用量、比例、含税单价 |
| 质保期 | 默认值 "6个月" |
| 价格类型 | 改为下拉框：空/新品价/维修价 |
| 新增弹窗按钮 | 点击弹出开机数量选择弹窗，按统计月份过滤 |
| 弹窗内容 | 表格展示机型+开机台数，单选后自动填充 |
| 统计月份 | 新增字段，使用月份选择器 |

---

## 5. 数据流图

```
156项 (base_material_156)
    │
    ├──→ 原始记录: 料号模糊查询 → 回填配件名称
    │
    ├──→ 超比统计: 料号/系统名称模糊查询 → 回填类别/系统名称/配件名称/单台机用量/比例/含税单价
    │         │
    │         ├──→ 结算机台数: 按月+料号 → 机台数
    │         ├──→ 送货记录: 按月+料号 → 送货数量 + 每日明细
    │         └──→ 原始记录: 按月+料号 → 上机数量 + 返修数量
    │
    └──→ 结算机台数: 料号模糊查询 → 回填类别/配件名称/单台机用量/比例/含税单价
              │
              └──→ 开机数量: 按月份 → 机型+开机台数（弹窗选择）
```

---

## 6. 实现顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| 1 | 数据库：新增 `base_material_156` 表 + 修改 `settlement_machine` 加 `stat_month` | - |
| 2 | 后端：完整的 `base_material_156` 模块（entity/mapper/service/controller） | 1 |
| 3 | 前端：156项页面（CRUD + 导入导出） | 2 |
| 4 | 后端：`original_record` 新增 lookup-156 接口 | 2 |
| 5 | 前端：原始记录料号 autocomplete + 配件名称回填 | 4 |
| 6 | 后端：`delivery_stats` auto-fill 接口 + 公式修正 | 2 |
| 7 | 前端：超比统计表单改造（autocomplete + auto-fill + 每日明细） | 6 |
| 8 | 后端：`settlement_machine` lookup-156 + machine-count by-month 接口 | 2 |
| 9 | 前端：结算机台数改造（autocomplete + auto-fill + 弹窗） | 8 |
| 10 | 后端：定时任务 `DeliveryStatsScheduler` | 6 |
| 11 | 全链路验证 | 全部 |
