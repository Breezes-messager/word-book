# 更新日志

## v1.0.0 — 2026-10-06

**首个完整可用版本**，在 Android 14 模拟器上完整跑通（含 DeepSeek 真实生成）。

### 功能

- **学习新词**：卡片正面只有单词 + 音标 + 发音按钮，点击或右滑翻面看中文释义 / 英释 / 例句 / 短语，四档评分
- **复习**：FSRS-6（Anki 现行算法，权重取自 open-spaced-repetition 官方实现）排程，先回忆再显示答案，完整复习日志
- **每日文章**：DeepSeek 生成，目标词高亮、逐段中文翻译、点击查词，断网 / 失败显示上次缓存
- **离线查词**：ECDICT 裁剪词典 + 词形还原表，精确 → 词形还原 → 前缀候选，全程不联网
- **设置**：每日新词数、每日复习上限、顺序 / 乱序、文章风格、API Key（含测试连接）、每日提醒、目标保持率
- **统计**：累计学词、连续打卡、30 天打卡条、卡片状态分布、最近文章
- **数据**：导出 JSON、清空全部数据

### 验证

- 29 个单元测试全部通过（含与官方 py-fsrs 的逐值比对、Robolectric 端到端集成测试）
- 模拟器实机：首次导入 5046 词、学习 / 复习闭环、词形还原 `actioned -> action`、飞行模式下查词正常、
  DeepSeek 真实生成文章（`POST /chat/completions 200`，4 段 158 词，5 个目标词全部出现）

### 修复（开发与联调过程中发现的问题）

| 问题 | 影响 |
|------|------|
| `newWordSession()` 用 `also` 返回原始对象，插入数据库生成的卡片 id 被丢掉 | 评分写不进数据库（Robolectric 集成测试发现） |
| ECDICT 的 `translation`/`definition` 里换行是字面量 `\n` | 查词弹层把 `\n` 直接显示出来 |
| `cachedNotice` 字段没在界面渲染 | 断网时不提示"当前为 X 月 X 日缓存的内容" |
| `lemmaNote` 拿还原后的词条和它自己比较 | "词形还原：actioned -> action" 永远不显示 |
| **DeepSeek 请求体缺少 `model` 字段** | 接口返回 **HTTP 422**，完全无法生成文章 |

最后一个是联调时踩的坑：kotlinx.serialization 默认 `encodeDefaults = false`，
会**跳过所有带默认值的字段**，而 `ChatRequest` 的 `model` / `temperature` / `stream` 都带默认值，
于是发出去的 body 里没有 `model`。修法是给 Retrofit 用的 `Json` 加上 `encodeDefaults = true`。

### 首版不做

文章 TTS、拼写 / 听音题型、云同步、多端、上架商店相关配置。
