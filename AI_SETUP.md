# 智慧集市智能客服：接口与配置

## 文件在哪里

本文档和知识库都在**后端项目**，不在 `market-frontend` 中：

```text
D:\SchoolTwo\fin\
├─ market-frontend\
└─ market-backend\
   ├─ AI_SETUP.md
   └─ src\main\resources\ai\faq.json  ← 本地知识库
```

本地知识库的完整路径为 `D:\SchoolTwo\fin\market-backend\src\main\resources\ai\faq.json`。目前有 15 条固定问答，每条包含 `id`、`question`、`keywords` 和 `answer`。修改该文件后需重新启动后端，服务启动时才会重新加载。它是关键词匹配的基础知识库，不会自动学习，也不是向量数据库。

## 接口概览

两个接口都要求登录，并在请求头中发送 `Authorization: Bearer <JWT>`。前端经 Axios 请求时会自动添加该请求头。以下路径是后端完整路径；前端 API 客户端的 `/ai/chat` 会由其 `baseURL` 补上 `/api`。

| 方法与路径 | 用途 | 当前前端使用情况 |
| --- | --- | --- |
| `GET /api/ai/suggestions` | 返回知识库前 6 个推荐问题 | 接口已实现；当前客服组件仍使用写在前端的 4 个推荐问题，未调用此接口 |
| `POST /api/ai/chat` | 提问并获取回复 | 客服组件正在使用 |

### 获取推荐问题

请求：

```http
GET /api/ai/suggestions
Authorization: Bearer <JWT>
```

成功响应示例（问题内容取自当前知识库）：

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    "如何浏览集市？",
    "如何申请摊位？",
    "摊位申请由谁审批？",
    "如何更换摊位？",
    "如何归还摊位？",
    "如何上架商品？"
  ]
}
```

### 发送聊天消息

请求：

```http
POST /api/ai/chat
Authorization: Bearer <JWT>
Content-Type: application/json

{"question":"如何申请摊位？","sessionId":null}
```

`question` 必填，不能全是空白，原始字符串最多 500 个字符；业务处理前还会去除首尾空白。`sessionId` 可为 `null`：首次提问时不提供会创建新会话，后续提问应传回响应中的 `sessionId`，以继续同一用户的短期上下文。

以下是**未配置外部模型、命中本地知识库时**的成功响应示例；`sessionId` 每次新会话的值不同：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "answer": "进入“探索集市”，选择开放的集市和空闲摊位，提交入驻申请；管理员审批通过后即可管理自己的摊位。申请状态以系统页面显示为准。",
    "source": "knowledge",
    "providerStatus": "not_configured"
  }
}
```

`data` 字段说明：

| 字段 | 含义 |
| --- | --- |
| `sessionId` | 当前会话标识。只在本机内存保存，约 30 分钟无活动后过期；后端重启后失效；不能用于访问其他用户的会话 |
| `answer` | 客服回复文本 |
| `source` | 本次回复的来源，见下表 |
| `providerStatus` | 外部模型的使用状态，见下表 |

`source` 可能值：

| 值 | 含义 |
| --- | --- |
| `catalog` | 实时查询数据库中的部分在售商品 |
| `ai` | 外部模型生成的回复 |
| `knowledge` | 外部模型未配置或不可用时，命中的本地固定问答 |
| `unknown` | 外部模型未配置或不可用，且本地问答未命中 |

`providerStatus` 可能值：

| 值 | 含义 |
| --- | --- |
| `not_needed` | 已由实时商品查询直接回答，未调用外部模型 |
| `online` | 外部模型调用成功 |
| `not_configured` | 未配置模型密钥，使用本地问答或未知问题提示 |
| `unavailable` | 模型调用失败或处于暂缓重试期，使用本地问答或未知问题提示 |

没有有效登录凭证时会被认证层拒绝（HTTP 401）；问题为空或超过长度限制时会被请求校验拒绝。客户端应同时检查 HTTP 状态和响应体中的 `code`，不要把失败响应当作正常回答。

## 回答顺序与降级

1. 商品列表类问题（例如“有哪些商品”）先查数据库：仅取已开放集市、已占用摊位的上架商品，最多展示 12 件。价格和库存以商品页面为准。数据库查询失败会继续走后续回答流程。
2. 其他问题会在本地知识库中按关键词找最多 3 条相关问答。**若配置了外部模型**，这些问答会作为参考内容发给模型，由模型生成回答；命中本地问答并不代表一定返回 `source=knowledge`。
3. 未配置模型，或模型调用失败、超时、暂缓重试时，返回最匹配的一条本地固定答案；无匹配时返回“暂时不在基础知识库中”的提示。

外部服务返回 4xx 后，当前后端进程约 10 分钟内暂缓重试；其他调用异常约 30 秒内暂缓重试。这是进程内状态，不会持久化。前端聊天请求等待上限为 23 秒；后端连接超时为 2 秒、读取超时为 18 秒。

## 配置外部模型

在**启动后端的进程环境**中配置，示例：

```powershell
$env:AI_API_KEY = '你的服务端密钥'
$env:AI_BASE_URL = 'https://api.deepseek.com'
$env:AI_MODEL = 'deepseek-chat'
```

`AI_API_KEY` 未配置时只使用实时商品查询及本地问答。`AI_BASE_URL` 和 `AI_MODEL` 可省略；代码默认值分别为 `https://api.deepseek.com` 和 `deepseek-chat`，但**默认值不会使未配置密钥的服务自动接入 DeepSeek**。接口请求会发送到 `<AI_BASE_URL>/chat/completions`。

也可在本机被 Git 忽略的后端配置文件中设置 `sensenova.api-key`、`ai.base-url`、`ai.model`。代码优先读取 `AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL`，再读取对应配置属性。密钥、接口根地址、模型 ID 必须属于同一服务，并且该账号有权调用该模型。不要把密钥写进前端、提交到 Git 或贴在日志中。

历史排障记录：此前使用 `https://token.sensenova.cn/v1` 时，旧模型 ID `sensenova-6.7-flash-lite` 的 404 响应内容是 `model is not found`，不是路径不存在。随后误切到另一套接口得到 403。改回原地址、换用**当时经账号模型列表验证可用**的 `sensenova-6.8-flash-lite` 后，实际调用成功。这只说明当时的配置；今后应先用服务商模型列表接口核对**当前账号**可用的模型，不能把历史验证当作永久保证。

## 数据与安全边界

客服目前提供公开业务指引和部分在售商品信息，不查询个人订单或审批状态，也不会代替用户执行操作。外部模型的提示词要求它不要索取敏感信息或猜测个人业务状态，但生成式回答仍可能出错；页面也提醒用户以实际业务页面为准。若以后要支持个性化查询，应另建受权限保护的业务接口，不要直接把未经筛选的数据库记录提供给模型。
