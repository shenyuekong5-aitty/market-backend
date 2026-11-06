# 智慧集市 · 后端

Spring Boot 3 / Java 17 后端，使用 MySQL、Redis、JWT、WebSocket 和 SSE，为智慧集市的用户、小贩及两类管理员提供业务接口。本仓库的 AI 客服只提供公开业务指引；前端位于同级目录 `market-frontend`。

## 本地运行与配置

项目附有 Maven Wrapper。先准备项目所需的数据库、Redis 及本机配置，再运行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

注册、重置密码和修改手机号默认使用阿里云短信并把验证码保存在 Redis。答辩现场如果不希望依赖短信网络，可仅在本机私有配置或进程环境中显式启用演示模式：

```powershell
$env:SMS_MOCK_ENABLED = 'true'
$env:SMS_MOCK_CODE = '123456'
```

对应 Spring 属性为 `sms.mock.enabled` 和 `sms.mock.code`。演示模式仍需 Redis，且绝不能在生产环境启用；接口不会把验证码返回给浏览器。

本仓库没有提交私有的 `src/main/resources/application.yml`。启动时需要按本地环境配置数据库、Redis 等服务；不要把密码、短信服务凭据或 AI 密钥提交到 Git。前端开发代理默认连接后端 `localhost:8088`，如果实际端口不同，请同步调整前端代理或环境变量。

AI API 可通过启动后端进程的环境变量配置：

```powershell
$env:AI_API_KEY = '你的服务端密钥'
$env:AI_BASE_URL = '与该密钥匹配的兼容 Chat Completions 接口根地址'
$env:AI_MODEL = '该账号可用的模型 ID'
```

三个值需要属于同一服务商且彼此匹配，缺少根地址或模型名时该服务不会启用，避免把密钥发送给错误的服务商。项目也支持读取本机私有配置中的 `sensenova.api-key`、`ai.base-url`、`ai.model`，但不要把真实值写进 README 或受版本控制的文件。密钥只供后端调用，不应提供给浏览器。

主接口失败时还可调用备用模型。备用密钥的持久化方式、优先级及工作目录要求见 [备用接口配置](docs/AI_BACKUP_CONFIG.md)。备用配置文件位于后端根目录，不进入 Git，也不会打包进 jar。

## 演示商品与公开橱窗

`GET /api/public/products` 返回开放集市中已占用摊位的上架商品，支持 `page`、`size` 参数，无需登录。开发环境可按需执行 [演示商品脚本](database/seed_market_atmosphere_products.sql)，脚本只为首个符合条件的摊位增添六款中文商品，重复运行不会重复插入。当前本机演示库已执行过一次；部署到正式环境前，应由摊主确认商品是否真实售卖，并核对价格、库存、图片。

六张风格统一的真实商品摄影图随服务发布，公开路径为 `/products/*.png`。种子脚本也会把旧插画地址迁移到 PNG 资源；正式售卖时，小贩仍应替换为实物照片。前端与后端分域部署时，请配置 `VITE_API_ORIGIN` 指向后端；同域反向代理需转发 `/products/` 和 `/images/`。

全新 MySQL 8 数据库先执行 [基础表结构](database/schema.sql)，再按需执行演示商品脚本。`schema.sql` 是完整基线，`db/migration/` 用于已有数据库的增量迁移；其中 `V20261106__fill_default_avatar.sql` 会为历史空头像账号补上系统默认头像，`V20261107__order_completion_and_integrity.sql` 会补齐交易完成时间、外键和核心检查约束。正式环境执行前应先备份并核对历史数据，约束创建失败时应修复孤儿数据，不能通过关闭约束绕过。

## 最小业务闭环

- 注册：公开注册只允许 `user` 或 `vendor`；管理员账号由超级管理员创建。没有自定义头像时使用 `/images/default-avatar.svg`。
- 入驻：小贩浏览开放集市并申请空闲摊位，由对应集市管理员审批；通过后才能维护摊位和商品。
- 交易：用户加入购物车并创建订单 → 模拟付款 → 小贩点击“备货完成” → 用户收到通知并到摊位取货 → 用户确认取货 → 订单完成并按完成时间进入收入统计。
- 取消：只有待付款订单可以直接取消并恢复库存。支付后不能直接取消，避免当前未接入退款系统时出现账实不一致。

当前“付款”只用于毕业设计中的状态流转，没有接入微信、支付宝或真实资金结算；界面、论文和演示说明中均应称为“模拟付款”。

## 管理审计与数据完整性

摊位审批以及管理员创建、状态切换、集市维护、摊位维护、系统通知等成功写操作会进入 `operation_log`。数据库基线为用户、集市、摊位、商品、购物车、订单、预订、通知和关注关系设置了外键，并对金额、库存、数量和主要状态设置检查约束。收入统计以 `complete_time` 为准；迁移前的历史完成订单使用最后更新时间回填。

## WebSocket 与 SSE 实时通知

登录后的通知默认由 WebSocket `/ws/notification/{userId}` 推送。当 WebSocket 不可用时，前端自动降级到 `GET /api/notifications/stream`。SSE 接口返回 `text/event-stream`，使用正常的 `Authorization: Bearer <token>` 请求头鉴权；连接成功事件为 `connected`，新通知事件为 `notification`，数据为 `new_notification`。WebSocket 恢复后前端会主动关闭 SSE，防止重复刷新。

两种实时通道都只提示“有新通知”，不直接携带通知内容。前端收到事件后，再调用 `/api/notifications` 和 `/api/notifications/unread-count` 获取当前用户的数据。SSE 连接由 `NotificationSseService` 管理，并在通知事务提交后推送事件。

## AI 客服接口

以下接口均需登录，请在请求头中携带 `Authorization: Bearer <token>`。统一响应格式为 `{"code":200,"message":"成功","data":...}`。

### `GET /api/ai/suggestions`

返回六条由本地知识库维护的常见问题，供前端快捷提问：

```json
{"code":200,"message":"成功","data":["如何浏览集市？","如何申请摊位？","有哪些商品？","如何预订商品？","如何查看订单？","如何修改个人信息？"]}
```

### `POST /api/ai/chat`

请求体：

```json
{"question":"如何申请摊位？","sessionId":null}
```

`question` 必填，最长 500 字符；首次请求的 `sessionId` 可为 `null`，之后使用上次响应中的值继续对话。响应示例（命中本地业务规则时）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "sessionId": "服务端生成的会话 ID",
    "answer": "进入“探索集市”，选择开放的集市和空闲摊位，提交入驻申请；管理员审批通过后即可管理自己的摊位。申请状态以系统页面显示为准。",
    "source": "knowledge",
    "providerStatus": "not_needed"
  }
}
```

| 字段 | 可能的值 | 含义 |
| --- | --- | --- |
| `source` | `catalog` | 数据库实时商品查询 |
| `source` | `ai` | 第三方模型回答 |
| `source` | `knowledge` | 本地知识库命中 |
| `source` | `unknown` | 超出业务范围或没有可靠答案 |
| `providerStatus` | `online` | 第三方模型调用成功 |
| `providerStatus` | `not_configured` | 未配置外部密钥 |
| `providerStatus` | `unavailable` | 外部服务失败或处于暂缓重试期 |
| `providerStatus` | `not_needed` | 实时商品、本地知识库或范围拦截已处理，无需调用外部模型 |

### 回答顺序与离线能力

1. 对“有哪些商品”“卖什么”等商品目录问题，优先从数据库读取已开放集市、已占用摊位中的上架商品，最多展示 12 件；价格和库存以商品页面为准。
2. 命中 `src/main/resources/ai/faq.json` 的平台规则和操作问题时，直接返回经过维护的固定答案，不再让外部模型自由改写。
3. 未命中知识库但仍属于集市经营范围的问题，才依次尝试主、备第三方模型。宣传文案只能使用用户明确提供的事实；服务端会拦截未经验证的质量、库存、优惠、营业及现场描述并要求模型安全重写。
4. 数学、天气等无关问题不会调用模型，也不会写入会话上下文。模型未配置、调用失败或安全重写仍不合格时返回 `unknown`，宁可说明无法可靠回答，也不编造业务事实。第三方 4xx 错误后暂缓重试约 10 分钟，其他调用异常约 30 秒。

本地知识库目前有 62 条问答，覆盖平台介绍、账号安全、四类角色、集市/摊位、商品、购物车、预订、取货式订单闭环、付款、运营管理、常见异常与简单寒暄。它按关键词和常见口语说法匹配，不是离线大模型；只对“那怎么取消？”“多久能通过？”等少量明确追问使用上一话题。会话属于当前用户，约 30 分钟无活动后过期，服务重启后不保留。完整回答边界见 [AI 客服回答规则](docs/AI_RESPONSE_RULES.md)。

### 维护本地知识库

编辑 [FAQ 文件](src/main/resources/ai/faq.json) 中的对象：

```json
{"id":"example","question":"示例问题？","keywords":["示例问法","另一种说法"],"answer":"经过业务核实的公开答案。"}
```

`id` 应唯一且稳定；`keywords` 放用户实际会说的短语和同义说法，避免只用过宽的单字词；`answer` 只写已由项目业务规则确认的公开信息。新增条目后运行 `.\mvnw.cmd test`，并检查相似问题是否误命中。快捷问题列表由 `AiKnowledgeService` 的 `SUGGESTION_IDS` 决定。知识库在应用启动时读取，修改后需要重新启动后端。

客服不会读取个人订单、账户或审批结果，也不会替用户支付、取消或审批。若未来需要个性化查询，应新增独立授权接口，而不是直接把个人数据库记录交给模型。
