# 更新日志

## v1.1.0 — 2026-10-06

### 新增：四套界面风格，可在 App 内随时切换

「设置 → 界面风格」新增 5 个选项，**点一下立即生效**（不用重启）：

| 选项 | 外观 | 特点 |
|------|------|------|
| 跟随系统 | Material You 动态取色 | 支持深色模式（原来的样子，默认） |
| 墨纸 | 米白纸感 + 朱红 | 宋体标题、直角卡片、1px 细线、下划线高亮 |
| 暗夜专注 | 近黑 + 薄荷荧光 | 巨型单词、细发光进度条、描边评分按钮、固定深色 |
| 薄荷圆润 | 奶油白 + 薄荷绿 | 大圆角卡片、柔和阴影、胶囊按钮、色块高亮 |
| 马克笔 | 纯白 + 荧光黄 | 粗黑描边、硬阴影、黄框标题、马克笔高亮 |

实现方式（不是简单换色，是「风格令牌」架构）：

- `ui/theme/AppStyles.kt`：一套 `AppStyleTokens`（卡片圆角/描边/硬阴影、进度条、评分按钮、
  高亮样式、字体族、区块标题处理方式）+ 5 套 ColorScheme / Shapes / Typography
- `ui/components/StyleComponents.kt`：`StyleCard` / `StyleProgress` / `StyleRatingRow` / `StyleSectionTitle`
- 组件通过 `CompositionLocal(LocalAppStyle)` 读取令牌，所以加风格不用改页面代码
- 设置存在 DataStore，`MainActivity` 观察它，切换后整棵树重组

### 验证

- 5 套风格全部在 Android 14 模拟器上跑通，逐套截图对比：`docs/style-samples/compare.png`
- 设计样板（HTML 稿）：`docs/style-samples/style-*.png`
- 深色风格下状态栏/导航栏图标会自动转浅色

## v1.0.4 — 2026-10-06

### 改进

- **APK 文件名带上版本号**：产物从 `app-debug.apk` 变成
  `背单词-v1.0.4-debug.apk`（release 同理），多个版本放在一起不会分不清。
  由 `app/build.gradle.kts` 里的 `applicationVariants` 自动生成，
  改 `versionName` 文件名自动跟着变。
- 现在 **文件名 / 应用内「设置」页显示 / git tag** 三处版本号完全一致，
  可以直接对照，不会再出现"装了新包却显示旧版本号"的困惑。

## v1.0.3 — 2026-10-06

### 修复

- **设置页底部的版本号一直显示 `背单词 v1.0`**（写死的字符串），
  APK 已经发到 1.0.2 了界面还是 v1.0，容易让人怀疑自己装错版本。
  改成从 `BuildConfig.VERSION_NAME` / `VERSION_CODE` 读取，以后改 `versionName` 界面自动跟着变，
  并显示 build 号方便和 APK 对应。

## v1.0.2 — 2026-10-06

### 修复（用户反馈）

- **「首页点『最近生成的文章』进文章页后，再点底部『首页』没反应」**

  原因：底部导航用的是官方示例里的 `saveState = true` / `restoreState = true`，
  而首页那张卡片是直接 `navigate("article")` 的（没有 popUpTo）。两者叠加后返回栈状态错乱，
  点首页时 `restoreState` 又把旧的返回栈恢复出来，界面依旧停在文章页。

  修法：把 tab 切换统一抽成 `ui/TabNavigation.kt` 里的 `switchTab()`，
  卡片、底部导航、「去设置」全部走同一套跳转（回到首页栈底 + launchSingleTop），
  不再保存 / 恢复 tab 状态，返回栈永远是 `[home]` 或 `[home, xxx]`。

### 新增

- `TabNavigationTest`（6 个 Robolectric 用例）复现并锁住这个 bug：
  卡片进文章页 → 点首页必须真的回到首页；反复切 tab 后返回键落在首页；
  连点同一个 tab 不堆层级。测试总数 29 → 35。

## v1.0.1 — 2026-10-06

### 修复

- **「重新生成今日文章」会先删掉今天的旧文章再请求接口**，一旦断网或接口报错，
  用户连原来那篇缓存都没有了。改成：先把新文章全部生成成功，再替换旧文章；
  失败时完整保留原文章，只在界面上给出中文错误提示和「重试」按钮。

### 验证

- 断网（飞行模式）点「重新生成」：重试 1 次后显示
  「网络不可用，检查一下网络连接（离线时仍可学习、复习、查词）」，
  下方**上一篇生成的文章仍然完整显示**（截图 docs/screenshots/11-生成失败降级.png）

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
