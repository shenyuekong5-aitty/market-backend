# AI 客服备用接口配置

回答顺序：实时在售商品查询 → 本地 `src/main/resources/ai/faq.json` → 主第三方模型 → 备用第三方模型 → 明确表示未知。固定业务规则不交给模型改写；只有知识库未命中但仍属于集市范围的问题才调用模型。主接口失败或处于暂缓重试期时才调用备用接口，主、备接口分别记录暂缓重试时间，互不影响。

备用接口必须兼容 OpenAI Chat Completions，并提供与密钥匹配的接口根地址和模型 ID。不要把密钥填进本文件、前端代码或 Git 管理的配置文件。

## 推荐：本机持久配置

在后端项目根目录创建或编辑 `ai-backup.local.properties`（与 `pom.xml` 同级）：

```properties
ai.backup.api-key=你自己的备用服务密钥
ai.backup.base-url=https://备用服务商的接口根地址/v1
ai.backup.model=该密钥可用的模型ID
```

此文件已列入 `.gitignore`，重启电脑或重新打开 IDE 后仍保留，也不会被打进后端 jar。启动时请将工作目录设为后端项目根目录；若从其他目录启动，使用下方环境变量。修改后需要重启后端。不要将真实内容复制到聊天、截图或提交记录中。文件缺失不会阻止服务启动。

## 临时方式：后端启动进程的环境变量

在启动后端的同一个 PowerShell 窗口中设置以下三个值，然后启动服务：

```powershell
$env:AI_BACKUP_API_KEY = '你自己的备用服务密钥'
$env:AI_BACKUP_BASE_URL = '备用服务的接口根地址，例如 https://服务商域名/v1'
$env:AI_BACKUP_MODEL = '该密钥可用的模型 ID'
.\mvnw.cmd spring-boot:run
```

`AI_BACKUP_BASE_URL` 是**根地址**，不要在末尾再写 `/chat/completions`；程序会自动追加。三项都配置完整时备用接口才会启用。只配置密钥不会启用。

如果你已经在本机未纳入 Git 的 `src/main/resources/application.yml` 中配置备用接口，也可继续使用，但不要与上述文件配置成不同服务商：

```yaml
ai:
  backup:
    api-key: 你自己的备用服务密钥
    base-url: 备用服务的接口根地址
    model: 该密钥可用的模型 ID
```

优先级为环境变量、`application.yml`、项目根目录的 `ai-backup.local.properties`。主接口原有的 `AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL` 配置保持不变。

对外接口仍是 `POST /api/ai/chat`，前端无需改地址。响应中的 `source=ai`、`providerStatus=online` 表示主接口或备用接口成功；当前响应不会向浏览器暴露具体服务商或密钥。服务端日志会区分主接口失败与备用接口失败，但不输出密钥。连接超时为 2 秒，单次读取超时为 9 秒；前端当前等待上限为 23 秒。
