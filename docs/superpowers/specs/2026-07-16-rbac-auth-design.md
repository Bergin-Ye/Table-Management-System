# RBAC 权限管理系统 — 设计文档

> 日期: 2026-07-16
> 分支: feature/rbac-auth

---

## 一、需求概述

在现有数据管理系统上增加基于角色的访问控制（RBAC），区分管理员和普通用户。

### 权限矩阵

| 功能 | 管理员 (admin) | 普通用户 (user) |
|------|:--:|:--:|
| 业务数据查看 | ✅ 全部 | ✅ 全部 |
| 业务数据新增 | ✅ | ✅ |
| 业务数据编辑/删除（自己创建的） | ✅ | ✅ |
| 业务数据编辑/删除（他人创建的） | ✅ | ❌ |
| 公司管理页面 | ✅ | ❌ |
| 操作记录页面 | ✅ | ❌ |
| 用户管理页面 | ✅ | ❌ |
| 导入/导出/模板下载 | ✅ | ✅ |

---

## 二、架构设计

在现有 JWT 认证基础设施上增量扩展，避免大重构：

```
请求 → AuthInterceptor(解析JWT含role) → UserContext(userId,username,realName,role)
     → ServiceHelper.isAdmin() / checkOwnershipOrAdmin()
     → Controller/Service 层校验
```

### 2.1 后端变更

**认证层：**
- `sys_user` 表新增 `role` VARCHAR(20) 列，默认值 `'user'`
- JWT token 中嵌入 `role` claim
- `UserContext` 增加 `role` 字段
- `ServiceHelper` 新增 `isAdmin()`, `requireAdmin()`, `checkOwnershipOrAdmin()` 方法

**权限控制点：**
- **页面级**：`CompanyController` 和 `OperationLogController` 的写操作加 `requireAdmin()`
- **数据级**：各业务 Service 的 update/delete 方法加 `checkOwnershipOrAdmin()`，admin 可操作所有数据，普通用户仅能操作 `createdBy` 为自己的记录
- **API 级**：新增 `/api/admin/users` 接口组，仅 admin 可调用

**数据追溯：**
- 5 张缺失 `created_by`/`updated_by` 的表补全字段：material, delivery_stats, settlement_machine, machine_detail, machine_count
- 所有 create 方法自动填入当前用户姓名
- 所有 update 方法自动更新 `updated_by`

**用户管理 API (`/api/admin/users`)：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 获取所有用户列表 |
| PUT | `/api/admin/users/{id}/role` | 修改用户角色 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| PUT | `/api/admin/users/{id}/reset-password` | 重置用户密码 |

### 2.2 前端变更

**状态管理：**
- auth store 新增 `role` 和 `isAdmin` 计算属性

**路由层：**
- 新增 `/user-management` 路由（用户管理页面）
- 操作记录和公司管理路由添加 `meta.requiresAdmin: true`
- 导航守卫检查角色，非管理员访问管理页面重定向到首页

**UI 层：**
- 侧边栏菜单根据 `isAdmin` 过滤：公司管理、操作日志、用户管理仅管理员可见
- 所有业务表格增加 `创建人` 列
- 新增用户管理页面（角色提升/降级、重置密码、删除用户）

---

## 三、数据库变更

### 3.1 sys_user 表
```sql
ALTER TABLE sys_user ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'user';
UPDATE sys_user SET role = 'admin' WHERE id = 1;
```

### 3.2 业务表补全字段
```sql
ALTER TABLE material ADD COLUMN `created_by` VARCHAR(50), ADD COLUMN `updated_by` VARCHAR(50);
ALTER TABLE delivery_stats ADD COLUMN `created_by` VARCHAR(50), ADD COLUMN `updated_by` VARCHAR(50);
ALTER TABLE settlement_machine ADD COLUMN `created_by` VARCHAR(50), ADD COLUMN `updated_by` VARCHAR(50);
ALTER TABLE machine_detail ADD COLUMN `created_by` VARCHAR(50), ADD COLUMN `updated_by` VARCHAR(50);
ALTER TABLE machine_count ADD COLUMN `created_by` VARCHAR(50), ADD COLUMN `updated_by` VARCHAR(50);
```

---

## 四、关键逻辑

### 4.1 数据归属校验
```java
public static void checkOwnershipOrAdmin(String recordCreatedBy, String action) {
    if (isAdmin()) return;  // 管理员可操作所有
    String currentUser = getCurrentUserName();
    if (!currentUser.equals(recordCreatedBy)) {
        throw new BizException("无权限" + action + "：只能操作自己创建的数据");
    }
}
```

### 4.2 管理员保护
- 不能删除最后一个管理员
- 管理员不能删除自己
- 管理员不能降级自己

---

## 五、文件变更清单

### 后端（17 个文件）
| 文件 | 操作 | 说明 |
|------|------|------|
| `init-data.sql` | 修改 | 建表语句加 role/created_by/updated_by |
| `scripts/migration-rbac.sql` | 新增 | 已有数据库的迁移脚本 |
| `entity/SysUser.java` | 修改 | 加 role 字段 |
| `entity/Material.java` | 修改 | 加 createdBy/updatedBy |
| `entity/DeliveryStats.java` | 修改 | 加 createdBy/updatedBy |
| `entity/SettlementMachine.java` | 修改 | 加 createdBy/updatedBy |
| `entity/MachineDetail.java` | 修改 | 加 createdBy/updatedBy |
| `entity/MachineCount.java` | 修改 | 加 createdBy/updatedBy |
| `interceptor/JwtUtil.java` | 修改 | generateToken 加 role 参数 |
| `interceptor/AuthInterceptor.java` | 修改 | UserContext 加 role |
| `common/ServiceHelper.java` | 修改 | 加权限检查方法 |
| `dto/LoginResultDTO.java` | 修改 | UserInfo 加 role |
| `service/AuthService.java` | 修改 | login 返回 role |
| `controller/AuthController.java` | 修改 | /me 返回 role |
| `mapper/SysUserMapper.java` | 修改 | 加用户管理查询方法 |
| `service/UserManageService.java` | **新增** | 用户管理业务逻辑 |
| `controller/UserManageController.java` | **新增** | 用户管理 API |
| 9 个 Service 类 | 修改 | 加归属校验 + createdBy/updatedBy |
| 5 个 Mapper 接口 | 修改 | INSERT/UPDATE 加 createdBy/updatedBy |
| `CompanyController.java` | 修改 | 加 admin 校验 |
| `OperationLogController.java` | 修改 | 加 admin 校验 |

### 前端（10 个文件）
| 文件 | 操作 | 说明 |
|------|------|------|
| `stores/auth.js` | 修改 | 加 role/isAdmin |
| `router/index.js` | 修改 | 加 admin 路由守卫 |
| `layout/AppLayout.vue` | 修改 | 菜单按角色过滤 |
| `api/admin.js` | **新增** | 用户管理 API |
| `views/admin/UserManageView.vue` | **新增** | 用户管理页面 |
| 5 个 View 文件 | 修改 | 加创建人列 |

---

## 六、测试要点

1. 管理员可访问所有页面，普通用户看不到管理页面
2. 普通用户编辑他人数据时后端返回错误
3. JWT token 中正确携带 role
4. 新注册用户默认为 user 角色
5. 管理员可提升/降级用户角色
6. 不能删除最后一个管理员
7. 操作日志正常记录所有操作
