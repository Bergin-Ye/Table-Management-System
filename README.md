# 金属厂数据管理系统

金属加工行业的生产数据管理平台，覆盖送货记录、维修记录、上机物料、超比统计、结算机台等核心业务场景。支持多公司隔离、RBAC 权限控制、语音解析录入、OCR 图片识别、Excel 批量导入导出。

---

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 后端框架 | Spring Boot | 3.3.0 |
| 开发语言 | Java | 21 |
| ORM | MyBatis + PageHelper | 3.0.3 / 2.1.0 |
| 数据库 | MySQL | 8.x |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| Excel | EasyExcel (Alibaba) | 3.3.4 |
| OCR | 通义千问 Qwen3.5-OCR (DashScope) | — |
| 前端框架 | Vue 3 + Vite | 3.5 / 8.x |
| UI 组件 | Element Plus | 2.14 |
| 状态管理 | Pinia | 4.0 |
| 路由 | Vue Router | 4.6 |
| HTTP 客户端 | Axios | 1.18 |

---

## 项目结构

```
System/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 配置
│   └── src/main/
│       ├── java/com/metal/
│       │   ├── MetalApplication.java           # 启动入口
│       │   ├── common/                         # 通用类（Result, PageResult, BizException）
│       │   ├── config/                         # 配置（Security, Interceptor, Web, Scheduling）
│       │   ├── controller/                     # 控制器（14个）
│       │   ├── dto/                            # 数据传输对象
│       │   ├── entity/                         # 数据库实体（10个）
│       │   ├── interceptor/                    # JWT 认证拦截器
│       │   ├── mapper/                         # MyBatis Mapper 接口 + XML
│       │   ├── scheduler/                      # 定时任务
│       │   └── service/                        # 业务逻辑层
│       └── resources/
│           ├── application.yml                 # 主配置（数据库、JWT、OCR）
│           ├── init-data.sql                   # 建表 + 初始数据
│           └── mapper/                         # MyBatis XML 映射文件
├── frontend/                         # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js                         # Vite 配置（含 /api 代理）
│   └── src/
│       ├── main.js                            # 应用入口
│       ├── App.vue
│       ├── api/                               # API 封装（每个模块一个文件）
│       ├── components/                        # 公共组件（SearchForm, ToolBar, PageHeader, LogDrawer）
│       ├── composables/                       # 组合式函数（useCrud, usePagination, useTableSelection）
│       ├── layout/                            # 布局组件（AppLayout）
│       ├── router/                            # 路由配置 + 导航守卫
│       ├── stores/                            # Pinia 状态（auth, company）
│       ├── utils/                             # 工具函数
│       └── views/                             # 页面视图（12个模块）
├── docs/                             # 文档
│   ├── 测试清单.md                              # 完整测试用例
│   ├── 测试报告.md                              # 最近一次测试结果
│   ├── 操作文档.md                              # 用户操作手册
│   ├── 需求文档.md                              # 原始需求
│   ├── 接口文档.md                              # API 接口说明
│   └── superpowers/specs/                     # 设计文档
├── scripts/                          # 工具脚本
│   └── test_runner.py                         # API 自动化测试脚本
└── 测试图片/                          # OCR 测试用图片
```

---

## 快速开始

### 环境要求

- **JDK** 21+
- **Maven** 3.8+
- **Node.js** 18+
- **MySQL** 8.0+

### 1. 初始化数据库

用 MySQL 客户端执行建表脚本：

```bash
mysql -u root -p < backend/src/main/resources/init-data.sql
```

或者直接复制 `init-data.sql` 内容在 Navicat/DBeaver 中执行。

### 2. 修改配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metal_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root          # 改成你的
    password: your_password # 改成你的

ocr:
  dashscope:
    api-key: your-api-key   # 通义千问 API Key（不使用 OCR 可以不填）
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，API 请求自动代理到后端 8080。

### 5. 注册管理员账号

首次启动后，调用注册接口创建管理员：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","realName":"管理员"}'
```

> **注意**：注册接口需要管理员权限。首次使用时，可以先在数据库 `sys_user` 表中手动插入一条 admin 记录（密码使用 BCrypt 加密），或临时注释掉 `AuthController.register()` 中的权限检查。

### 6. 访问系统

打开浏览器访问 `http://localhost:5173`，使用刚才注册的账号登录。

---

## 功能模块

### 业务模块（所有用户可访问）

| 模块 | 路由 | 说明 |
|------|------|------|
| 送货记录 | `/delivery-record` | 物料送货记录的增删改查、搜索、排序、Excel 导入导出、语音解析录入 |
| 维修记录 | `/original-record` | 维修记录管理，支持 OCR 图片识别、跨天工时自动计算、过保查询、单据号 |
| 超比统计 | `/delivery-stats` | 送货超比例统计，支持自动填充（从156项回填）、当月数据批量刷新 |
| 结算机台数 | `/settlement-machine` | 各机型结算机台数管理，支持从156项自动回填 |
| 机型明细 | `/machine-detail` | 厂房-机台号-机台品牌的对应关系管理 |
| 开机数量 | `/machine-count` | 各机型开机数量与占比统计，支持按月管理 |
| 物料表 | `/material` | 物料基础信息管理 |
| 156项 | `/base-material-156` | 156项基础物料主数据，跨公司唯一性约束 |

### 管理员模块（仅 admin 可见）

| 模块 | 路由 | 说明 |
|------|------|------|
| 上机物料 | `/machine-material` | 上机物料记录管理 |
| 操作日志 | `/operation-log` | 所有用户的操作记录审计 |
| 公司管理 | `/company` | 多公司（租户）管理 |
| 用户管理 | `/user-management` | 用户角色管理、密码重置 |

---

## 权限说明

系统采用 RBAC 模型，角色分为 **admin**（管理员）和 **user**（普通用户）：

| 操作 | admin | user |
|------|:-----:|:----:|
| 业务数据查看 | ✅ | ✅ |
| 业务数据新增 | ✅ | ✅ |
| 编辑/删除自己创建的数据 | ✅ | ✅ |
| 编辑/删除他人创建的数据 | ✅ | ❌ |
| 上机物料 | ✅ | ❌ |
| 操作日志 | ✅ | ❌ |
| 公司管理 | ✅ | ❌ |
| 用户管理 | ✅ | ❌ |

权限校验分三层：
1. **路由守卫**（`router/index.js`）：前端路由级拦截
2. **菜单显隐**（`AppLayout.vue`）：`v-if="authStore.isAdmin"` 控制菜单项可见性
3. **API 拦截**（`ServiceHelper.requireAdmin()`）：后端接口级权限校验

---

## 核心特性

### 语音解析

所有业务模块的新增表单都支持语音/文字解析。将口语化文本粘贴到语音输入框，点击解析即可自动填充表单字段。

示例输入（送货记录）：

```
日期2026年7月21日 类别备件 物料名称丝杆 规格型号M8 物料编码ABC 数量5 品牌FANUC 厂房A
```

示例输入（维修记录）：

```
日期2026年7月21日 班次白班 厂房A 机台号K25 机型FANUC 诊断人张三 维修人李四 确认人王五 报修时间22时 开始时间23时 结束时间02时 故障现象主轴异响 维修描述更换丝杆 料号2212673-0461 配件名称丝杆 数量1 上机物料号M001 下机物料号M002 单据号DOC001 备注无
```

系统基于关键词锚点匹配（日期、班次、厂房、料号、单据号 等），本地解析，不依赖外部 API。

### OCR 图片识别

维修记录模块支持上传设备维修单据图片，通过通义千问 Qwen3.5-OCR 自动识别并填充字段。需要使用有效的 DashScope API Key。

### Excel 导入导出

送货记录、维修记录、超比统计等模块均支持：
- **模板下载**：下载标准 Excel 模板
- **批量导入**：按模板填写后一键导入
- **数据导出**：按当前筛选条件导出 Excel

### 跨天工时计算

维修记录中，当结束时间跨越午夜时，系统自动处理日期：
- 输入：开始时间 `23:00`，结束时间 `02:00`
- 自动计算：`endTime = 次日 02:00`，`repairHours = 180分钟`，`downtimeHours = 240分钟`

### 百分比处理

超比统计、结算机台数等模块的比例字段采用统一约定：
- 前端输入/显示：`15` → 列表显示 `15%`
- 数据库存储：`0.15`（小数形式）
- 编辑回显：`15`（自动 ×100）

### 多公司隔离

每个业务表都有 `company_id` 字段，切换 Header 中的公司下拉框后，所有数据自动按公司过滤。

---

## API 概览

| 路径 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/auth/login` | POST | 用户登录 | 公开 |
| `/api/auth/register` | POST | 注册用户 | admin |
| `/api/auth/me` | GET | 获取当前用户信息 | 登录 |
| `/api/company` | GET | 公司列表 | 登录 |
| `/api/company` | POST | 新增公司 | admin |
| `/api/delivery-record` | GET | 送货记录列表/搜索 | 登录 |
| `/api/delivery-record` | POST | 新增送货记录 | 登录 |
| `/api/delivery-record/{id}` | PUT | 编辑送货记录 | 登录 |
| `/api/delivery-record/batch-delete` | POST | 批量删除 | 登录 |
| `/api/delivery-record/import` | POST | Excel 导入 | 登录 |
| `/api/delivery-record/export` | GET | Excel 导出 | 登录 |
| `/api/delivery-record/template` | GET | 下载模板 | 登录 |
| `/api/original-record` | GET/POST | 维修记录 CRUD | 登录 |
| `/api/original-record/lookup-warranty` | GET | 过保查询 | 登录 |
| `/api/original-record/lookup-156` | GET | 156项回填查询 | 登录 |
| `/api/delivery-stats` | GET/POST | 超比统计 CRUD | 登录 |
| `/api/delivery-stats/auto-fill` | GET | 从156项自动填充 | 登录 |
| `/api/delivery-stats/batch-refresh` | POST | 批量刷新当月数据 | 登录 |
| `/api/settlement-machine` | GET/POST | 结算机台数 CRUD | 登录 |
| `/api/machine-count` | GET/POST | 开机数量 CRUD | 登录 |
| `/api/machine-detail` | GET/POST | 机型明细 CRUD | 登录 |
| `/api/material` | GET/POST | 物料表 CRUD | 登录 |
| `/api/base-material-156` | GET/POST | 156项 CRUD | 登录 |
| `/api/machine-material` | GET/POST | 上机物料 CRUD | **admin** |
| `/api/operation-log` | GET | 操作日志 | **admin** |
| `/api/admin/users` | GET | 用户列表 | **admin** |
| `/api/admin/users/{id}/role` | PUT | 修改用户角色 | **admin** |
| `/api/admin/scheduler` | GET/PUT | 定时任务管理 | **admin** |
| `/api/voice-parse` | POST | 语音/文字解析 | 登录 |
| `/api/ocr/recognize` | POST | OCR 图片识别 | 登录 |

完整接口文档见 [docs/接口文档.md](docs/接口文档.md)。

---

## 数据库表

| 表名 | 说明 |
|------|------|
| `company` | 公司（多租户） |
| `sys_user` | 系统用户 |
| `operation_log` | 操作日志 |
| `delivery_record` | 送货记录 |
| `original_record` | 维修记录（含单据号字段 `document_no`） |
| `machine_material` | 上机物料 |
| `delivery_stats` | 超比统计主表 |
| `delivery_stats_daily` | 超比统计每日明细 |
| `settlement_machine` | 结算机台数 |
| `machine_detail` | 机型明细 |
| `machine_count` | 开机数量 |
| `material` | 物料表 |
| `base_material_156` | 156项基础物料 |

建表 SQL 见 `backend/src/main/resources/init-data.sql`。

---

## 运行测试

```bash
# API 自动化测试（需要后端正在运行）
cd scripts
python test_runner.py
```

测试覆盖：登录认证、权限控制、公司隔离、所有模块的 CRUD、跨天工时计算、百分比存储、跨公司唯一性、语音解析（8表）。

测试报告见 [docs/测试报告.md](docs/测试报告.md)。

---

## 构建部署

### 后端打包

```bash
cd backend
mvn clean package -DskipTests
java -jar target/metal-system-1.0.0.jar
```

### 前端打包

```bash
cd frontend
npm run build
# 产出在 dist/ 目录，部署到 Nginx 或拷贝到后端 static 目录
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /path/to/frontend/dist;
    index index.html;
    try_files $uri $uri/ /index.html;

    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 文档索引

| 文档 | 位置 | 说明 |
|------|------|------|
| 需求文档 | [docs/需求文档.md](docs/需求文档.md) | 原始业务需求 |
| 操作文档 | [docs/操作文档.md](docs/操作文档.md) | 用户操作手册 |
| 接口文档 | [docs/接口文档.md](docs/接口文档.md) | API 接口说明 |
| 测试清单 | [docs/测试清单.md](docs/测试清单.md) | 完整测试用例 |
| 测试报告 | [docs/测试报告.md](docs/测试报告.md) | 最近一次测试结果 |
| RBAC 设计 | [docs/superpowers/specs/2026-07-16-rbac-auth-design.md](docs/superpowers/specs/2026-07-16-rbac-auth-design.md) | 权限系统设计 |
| 156项设计 | [docs/superpowers/specs/2026-07-18-156-items-and-auto-fill-design.md](docs/superpowers/specs/2026-07-18-156-items-and-auto-fill-design.md) | 156项 + 自动填充设计 |
