# 后端快速导览

这份导览按当前代码编写。先沿一条请求读通，再按业务模块扩展；不要把目录名当成权限边界，真正的访问控制以 `SecurityConfig` 和业务校验为准。

## 30 分钟阅读路线

| 时间 | 阅读位置 | 读完应能回答 |
| --- | --- | --- |
| 0–5 分钟 | `MarketApplication`、`config/SecurityConfig`、`config/JwtAuthenticationFilter` | 服务如何启动？哪些接口公开？Bearer Token 如何变成当前用户？ |
| 5–10 分钟 | `controller/common/AuthController`、`service/impl/UserServiceImpl`、`common/UserRole` | 登录如何校验角色？为什么有四类身份？旧 `admin` 如何兼容？ |
| 10–18 分钟 | `controller/vendor/VendorProductController` → `service/impl/ProductServiceImpl` → `mapper/ProductMapper` → `entity/Product` | 一个请求怎样经过 Controller、Service、Mapper 到数据库？ |
| 18–25 分钟 | `controller/admin/BoothApplyController` → `service/impl/BoothApplyServiceImpl`；`controller/user/OrderController` → `service/impl/OrderServiceImpl` | 入驻审批、下单的核心状态变更在哪里？ |
| 25–30 分钟 | `controller/common/AiChatController` → `service/AiChatService`、`AiKnowledgeService`、`AiCatalogService` | AI 怎样选择实时商品、外部模型和本地知识库？ |

## 一张图看请求路径

```text
浏览器 /api/**
    ↓
SecurityConfig + JwtAuthenticationFilter
    ↓
controller/{common,user,vendor,admin}（参数与响应）
    ↓
service 接口 → service/impl（业务规则和事务）
    ↓
mapper（MyBatis-Plus）→ MySQL
    └─ 可选：NotificationService
                  ├─ NotificationEndpoint（WebSocket，默认）
                  └─ NotificationSseService（SSE，降级通道）
```

`common/Result` 是通用响应封装。`entity` 是数据库实体，`dto` 是部分接口使用的传输对象。`scheduler/OrderScheduler` 处理定时任务。`src/main/resources/ai/faq.json` 是 AI 本地问答，不是数据库表。

## 按角色找代码

| 身份 | 主要入口 | 重点业务 |
| --- | --- | --- |
| 普通用户 `user` | `controller/user` | 浏览、购物车、订单、预订、关注 |
| 小贩 `vendor` | `controller/vendor` | 入驻、摊位、商品、订单、收入 |
| 集市管理员 `market_admin` | `controller/admin` | 所属集市、摊位、审批、运营数据 |
| 超级管理员 `super_admin` | `controller/admin` | 平台管理员管理与全平台审批 |

`controller/common` 放登录、账户、用户资料、通知、上传、公开商品和 AI 等跨角色接口；“common”不等于匿名可访问。角色授权集中在 `config/SecurityConfig`，旧账号的 `role=admin` 由 `common/UserRole` 映射为两种管理员之一。服务方法内仍可能检查数据归属，不能只看 URL 判断权限。

## 三条最值得跟踪的调用链

1. 登录：`AuthController.login` → `UserServiceImpl.login` → `JwtUtils`；后续请求由 `JwtAuthenticationFilter` 从 Bearer Token 还原用户并检查角色是否变化。
2. 下单：`user/OrderController.createFromCart` → `OrderServiceImpl.createOrdersFromCart` → 购物车、商品、摊位、订单相关 Mapper；状态依次为待付款、已付款、待取货、已完成，取消仅允许在待付款阶段。
3. 入驻审批：`admin/BoothApplyController.approve` → `BoothApplyServiceImpl.approve` → 申请/摊位/用户状态更新，并写通知；默认通过 WebSocket 提醒，连接不可用时前端使用 SSE 降级通道。

AI 的额外调用链：`AiChatController.chat` → `AiChatService.chat`。先尝试实时商品查询，再匹配 FAQ 并直接返回固定业务规则；只有 FAQ 未命中但仍属于集市范围的问题才按配置调用兼容 Chat Completions 的主、备模型。文案回答还会经过事实边界检查和安全重写。接口不读取个人订单和审批状态。详细请求与响应见根目录 `README.md`。

## 当前结构的边界与改动位置

现有分层基本清楚，不需要为了“整洁”而移动全部类或重命名 API。理解成本主要来自 `OrderServiceImpl`、`BoothApplyServiceImpl`、`UserServiceImpl` 体量较大，以及若干控制器重复读取当前用户。后续若要拆分，应按业务子流程抽取并先补测试；不要直接挪动事务边界或改变接口路径。AI 相关服务和 FAQ 目前有未提交的本地修改，整理时需保留。

修改功能时优先按这个顺序定位：接口路径 → 对应 Controller → Service 接口与实现 → Mapper/实体 → `SecurityConfig` 授权规则 → 测试。全新数据库先执行 `database/schema.sql`，演示商品使用 `database/seed_market_atmosphere_products.sql`，已有数据库的角色迁移脚本位于 `db/migration/`；私有 `application.yml` 不应提交。

## 本地验证

在后端根目录运行 `./mvnw.cmd test`（PowerShell 用 `.\mvnw.cmd test`）。重点测试在 `src/test/java/com/market`；构建通过不等于数据库、Redis 和第三方 AI 已连接，运行环境还需要各自配置。前端位于同级 `market-frontend`，其 `src/api` 可反查实际请求路径。
