# 90 · 外部资料索引

> **本文件职责**：把本工程会用到的**外部链接**与**本地参考资料**整理成一份带分类、带用途说明、带可达性的索引。
> 目的：任何人在动手前先在这里找到「该信谁、去哪查、能不能打开」，避免重复搜索与凭印象写 API。
> 可达性实测日期：**2026-10-03**（本机 Windows + `curl.exe -sL -k`）。
> 合规前提：**所有外部资料只用于核实「玩法事实」与「避免重名」，严禁抄代码与素材**——见 [07-compliance.md](07-compliance.md)。

## 可达性图例

| 标记 | 含义 |
|---|---|
| ✅ **可访问** | 本机实测 HTTP 200 |
| ⚠️ **需代理/镜像** | 直连不可靠，需走 `ghproxy.net` / `ghfast.top` / `gh-proxy.com` / `raw.` / `codeload.` 等通道（见 [20-environment.md](20-environment.md) 第六节） |
| ⛔ **被拦截** | 实测 403，本机打不开（多为 Cloudflare 拦截） |
| ❓ **未验证** | 本次没测，或测了但结论不确定 |

---

## 一、原模组资料（玩法事实来源）

> **唯一允许的用法**：只提取**玩法机制、数值、系统结构**，以及比对**名称清单**以避免重名。
> `.java` 源码**禁止阅读**（见 [07-compliance.md](07-compliance.md) 第四节净室流程）。

| 资料 | 链接 | 能提供什么 | 可达性 |
|---|---|---|---|
| **中文资料页（首选）** | https://www.mcmod.cn/class/628.html | 949 字以上的中文玩法描述：泰坦种类、召唤物体系、武器道具、模式与数值概述。**这是我们的玩法事实主来源** | ✅ 实测 200 |
| 源码仓库（本体） | https://github.com/Mrbt0907/The-Titans-Mod | 1.12.2 / `0.6.3-indev`。**只用于看资源文件名清单**，核实「发行版有哪些内容」与避免重名 | ✅ 网页 200；clone 见 1.1 |
| 依赖库（MrbtUtil 3.0） | https://github.com/Mrbt0907/Minecraft-Adventures-Core | 本体硬依赖的库（modid `mrbtutil`）。**只用于理解它提供了哪些通用能力**，不读实现 | ✅ 实测 200 |
| CurseForge 项目页 | https://www.curseforge.com/minecraft/mc-mods/the-titans-mod | 发行版下载页、更新日志、版本对应关系 | ⛔ 实测 **403**（本机打不开） |
| mcmod 站内搜索 | https://search.mcmod.cn/s?key=%E6%B3%B0%E5%9D%A6%E7%94%9F%E7%89%A9 | 找不到具体页面时按关键词检索 mcmod 条目 | ✅ 实测 200 |

**⚠️ 合规标注（硬性）**：上面两个 GitHub 仓库**都没有 LICENSE 文件**，即默认保留所有权利。因此：

- ✅ 可以做：读玩法描述、看文件名清单、比对名称、借鉴机制与数值思路；
- ❌ 不可以做：复制/改写任何 `.java`、反编译 jar、使用其贴图/音效/模型/语言文件、沿用 `thetitans` 与 `net.mrbt0907.*` 命名。
- 详细红线清单与禁用名表见 [07-compliance.md](07-compliance.md) 第三节。

### 1.1 仓库下载通道（实测）

github.com 的 DNS 被劫持，**不要假设 `git clone` 一定可用**（依赖本地加速器状态）：

```powershell
# 首选：codeload 下整包 zip（实测 14.9 MB 成功）
curl.exe -sL -k -o E:\titanmod\.tmp\titans.zip `
  "https://codeload.github.com/Mrbt0907/The-Titans-Mod/zip/refs/heads/main"

# 依赖库同理（默认分支是 master）
curl.exe -sL -k -o E:\titanmod\.tmp\mac.zip `
  "https://codeload.github.com/Mrbt0907/Minecraft-Adventures-Core/zip/refs/heads/master"

# 只列文件树，先看清有什么（不下内容）
curl.exe -sL -k "https://data.jsdelivr.com/v1/packages/gh/Mrbt0907/The-Titans-Mod@main?structure=flat" `
  -o E:\titanmod\.tmp\filetree.json
```

| 通道 | 实测 |
|---|---|
| `codeload.github.com` zip 下载 | ✅ 200 / 14.9 MB |
| `raw.githubusercontent.com` 单文件 | ✅ 200 |
| `data.jsdelivr.com` 文件树 | ✅ 200 |
| `ghproxy.net` / `ghfast.top` / `gh-proxy.com` 代理 | ✅ 200 |
| `git ls-remote` / `git clone`（github.com 直连） | ✅ 实测成功，**但取决于本机加速器是否在跑**，见 [20-environment.md](20-environment.md) 6.1 |

---

## 二、同类项目（竞品与参考）

> **用法**：判断差异化方向、避免撞名、了解「1.21.x 上重实现泰坦」这条路是否已被走通。
> **纪律**：这些项目**全部 All Rights Reserved、无公开源码**——不读其代码、不反编译其 jar、不使用其素材（见 [07-compliance.md](07-compliance.md) 第七节）。
> 下载量为 **2026-10-03** 经 Modrinth API 抓取，属会变的平台数据，引用时请注明日期。

| 项目 | 链接 / ID | 平台与版本 | 下载量 | 能提供什么 | 可达性 |
|---|---|---|---|---|---|
| **Neo The Titans（主要竞品）** | https://modrinth.com/mod/neo-the-titans （ID `XGi9Z8am`） | NeoForge **1.21.4** | **936,713** | 最直接的竞品：它做「优化动画 + 平衡数值 + 加指令」，**我们的差异化就是避开它做的方向**（见 [00-overview.md](00-overview.md) 3.1）。授权 `LicenseRef-All-Rights-Reserved`、`source_url` 为空，最后更新 2026-09-24（API 实测） | ✅ 200 |
| The Titans: Old Dream (Unofficial) | https://www.mcmod.cn/class/27006.html | 1.20.1 非官方移植（CurseForge） | — | 证明「跨版本重实现」可行；看它的取舍（哪些机制被砍） | ✅ 200 |
| 新泰坦生物：撼世之物 | https://www.mcmod.cn/class/20584.html | 中文圈新泰坦项目 | — | 中文圈同类项目的命名与宣传口径，用于**避免重名与撞定位** | ✅ 200 |
| Neo Titans Variant | https://modrinth.com/mod/neo-titans-variant （ID `ThKruwh3`） | NeoForge 1.21.4 | 约 4.6 万 | Neo The Titans 的衍生/变体，看生态怎么分化 | ✅ 200（ID 由 API 核实） |
| Ben's Titans | https://modrinth.com/mod/bens-titans （ID `8dUviuGN`） | Forge 1.20.1 | 约 9 千 | 小型同类实现的规模参考 | ✅ 200（ID 由 API 核实） |
| TheTitansNeo | https://modrinth.com/mod/thetitansneo （ID `Ym1A9kza`） | NeoForge **1.21 / 1.21.1** | 约 **18 万** | **同版本段（1.21.1）的竞争者**，比 Neo The Titans 更贴近我们的目标版本，值得单独研究 | ✅ 200（ID 由 API 核实） |
| The Titans Mod（原版发行） | https://modrinth.com/mod/the-titans-mod （ID `4px3rAEy`） | Forge 1.7.10 / 1.8 | 约 13 万 | 原作者的发行版页；**下载量数据可用于说明原版只到 1.8** | ✅ 200（ID 由 API 核实） |

**用 Modrinth API 自己查（实测可用，比翻网页快）**：

```powershell
# 单个项目：拿 slug / 标题 / 下载量 / 加载器 / 游戏版本 / 授权
curl.exe -sL -k "https://api.modrinth.com/v2/project/XGi9Z8am"

# 关键词搜索：一次看清同类项目全貌
curl.exe -sL -k "https://api.modrinth.com/v2/search?query=titans&limit=20"
```

> 注意：`api.modrinth.com` 的根路径与某些查询会返回 404/400，**属正常**，要带具体 project id 或 `?query=`。

---

## 三、技术文档（写代码时查这里）

> **纪律**：遇到「1.21.1 该怎么写」时，只查 **NeoForge / Minecraft 官方资料**，不查原模组源码（见 [07-compliance.md](07-compliance.md) 第四节第 4 条）。

### 3.1 NeoForge 官方文档

| 资料 | 链接 | 能提供什么 | 可达性 |
|---|---|---|---|
| NeoForge 文档首页 | https://docs.neoforged.net/ | 全部官方文档的入口与目录 | ✅ 实测 200 |
| 入门（getting started） | https://docs.neoforged.net/docs/gettingstarted/ | 工程骨架、`build.gradle`、`neoforge.mods.toml`、运行配置的官方口径 | ✅ 实测 200 |
| 实体（entities） | https://docs.neoforged.net/docs/entities/ | `EntityType` 注册、属性、AI 目标、生成规则——**我们核心模块的官方依据** | ✅ 实测 200 |
| 方块实体 | https://docs.neoforged.net/docs/blockentities/ | 带状态的方块逻辑 | ✅ 实测 200 |
| 网络（networking） | https://docs.neoforged.net/docs/networking/ | 自定义 payload、客户端/服务端同步——**我们核心模块的官方依据** | ✅ 实测 200 |
| 客户端模型 | https://docs.neoforged.net/docs/resources/client/models/ | 模型 JSON 规范（1.21.x 的 `item/generated` 已废弃等） | ✅ 实测 200 |
| NeoForge 主站 | https://neoforged.net/ | 版本发布、公告 | ✅ 实测 200 |
| NeoForm 项目页 | https://projects.neoforged.net/neoforged/neoform | NeoForm（反编译/重编译管线）的版本与产物说明——**排查 zipfs 问题时的背景资料** | ✅ 实测 200 |

> 注意：`https://docs.neoforged.net/docs/`（目录根）实测 **404**，要访问具体章节路径；`/docs/` 下的路径可能随文档重构变动，**以首页导航为准**。

### 3.2 ModDevGradle / NeoForm（工具链）

| 资料 | 链接 | 能提供什么 | 可达性 |
|---|---|---|---|
| ModDevGradle 仓库 | https://github.com/neoforged/ModDevGradle | Gradle 插件的官方用法、`neoForge { }` 全部属性（含 `neoForge.neoFormRuntime.*` 这类属性名） | ⚠️ 需代理/镜像（本机 github 直连不稳），备选见下行 |
| ModDevGradle README（单文件直取） | https://raw.githubusercontent.com/neoforged/ModDevGradle/main/README.md | 同上，但走 raw 通道，**本机实测可靠** | ✅ 实测 200 |
| Gradle 插件门户 | https://plugins.gradle.org/m2/ | `net.neoforged.moddev` 插件的坐标与版本 | ✅ 实测 200 |
| NeoForge Maven（releases） | https://maven.neoforged.net/releases/ | NeoForge / neoform-runtime / moddev-gradle 全部产物 | ✅ 实测 200 |
| NeoForm Runtime 版本目录 | https://maven.neoforged.net/releases/net/neoforged/neoform-runtime/ | 本机用的是 **2.0.24**；要升级/回退时在这里选版本 | ✅（同一 Maven） |
| NeoForm 详细日志开关 | 本地依据：`example-mod/gradle.properties` 的 `neoForge.neoFormRuntime.verbose=true` | 打开后 NeoForm 才会输出真实的 javac 报错——**不打开只能看到 `null`** | 本地已配 |
| ECJ 回退开关 | 同上：`neoForge.neoFormRuntime.useEclipseCompiler=true` | 绕开工作区 JDK 的 zipfs 只读问题（见 [20-environment.md](20-environment.md) 第四节） | 本地已配 |

> `neoForge.neoFormRuntime.*` 属性名对应 `NeoFormRuntimeExtension.getVerbose()` 等方法，**改动前先核对 ModDevGradle README 的当前版本**（我们锁的是 2.0.143）。

### 3.3 Minecraft Wiki

| 资料 | 链接 | 能提供什么 | 可达性 |
|---|---|---|---|
| 主站 | https://minecraft.wiki/ | 原版机制百科 | ⚠️ 网页实测 **403**（Cloudflare 拦截），但 **API 可用** |
| 实体条目 | https://minecraft.wiki/w/Entity | 实体基础机制、碰撞箱、伤害与无敌帧等原版行为 | ⚠️ 网页 403 |
| Wiki API（**推荐通道**） | https://minecraft.wiki/api.php?action=query&format=json&titles=Entity | 用 API 取条目内容，**实测 200**；适合脚本化查证 | ✅ 实测 200 |
| 数据驱动内容（维度/生物群系等） | https://minecraft.wiki/w/Data-driven （网页 403，改用 `api.php?action=query&format=json&titles=Data-driven`） | 数据包 JSON 的结构与字段——对应我们「数据驱动优先」的目标 | ⚠️ 网页 403 / API 待验证 ❓ |

> 结论：**Minecraft Wiki 一律走 `api.php`**，不要直接开网页。

### 3.4 NeoForge 源码（核对 API 的最终依据）

任何「API 到底叫什么、参数是什么」的问题，最终都以 **NeoForge 源码 jar** 为准，而不是博客或教程。

| 内容 | 位置 |
|---|---|
| NeoForge 源码 jar（本地已缓存） | `E:\titanmod\.gradle-home\caches\modules-2\files-2.1\net.neoforged\neoforge\` |
| `21.1.252` 源码 jar | `…\neoforge\21.1.252\50b7fd4f93602d7f0842cee9adc35d936198bae3\neoforge-21.1.252-sources.jar` |
| `21.1.248` 源码 jar | `…\neoforge\21.1.248\f0378c9b210dfc258170b6412a85fdffe4aea7fe\neoforge-21.1.248-sources.jar` |
| moddev 配置（110 项 classpath 的来源） | `…\neoforge\21.1.252\23fb1fe574ac6e6c1803425f8e817a4535cc2ced\neoforge-21.1.252-moddev-config.json` |
| 在线取（本地缓存丢了时） | https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248-sources.jar ✅ 实测 200 |

```powershell
# 在线补一份源码 jar（实测 1.79 MB）
curl.exe -sL -k -o E:\titanmod\.tmp\neoforge-21.1.248-sources.jar `
  "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248-sources.jar"
```

> **注意**：`21.1.248` 与 `21.1.252` 两份都在缓存里，但 `example-mod/build.gradle` 实际用的是 **21.1.252**。核 API 前先确认目标版本，见 [20-environment.md](20-environment.md) 1.2。

---

## 四、本地参考

### 4.1 技能文档 `minecraft-modding`（`E:\titanmod\.dsh\skills\minecraft-modding\`）

这是本机自带的开发技能库（v0.1.0）。**读顺序建议：先 `SKILL.md` 定路由，再按内容类型进具体文件。**

| 文件 | 管什么 | 状态 |
|---|---|---|
| `SKILL.md` | 技能总入口：能力清单、路由、失败处理 | ✅ |
| `references/00-intake.md` | **唯一路由表**：探测规则（不猜）、三档模式、「内容 → 该读哪个文件」映射、能力清单与降级规则 | ✅ |
| `references/01-glossary.md` | 术语表中英对照（modid / 注册表 / 副作用 / 物理端与逻辑端 / 方块实体…） | ✅ |
| `references/02-common-concepts.md` | 运行机制通识：注册、副作用与分端、`ResourceLocation`、构建流程、加载器差异概览 | ✅ |
| `references/03-assets.md` | 贴图 / 模型 / blockstate / `pack.mcmeta` 规范（避免游戏内黑紫方块） | ✅ P1 最小版 |
| `10-modern-lane/README.md` | **现代线导航**：文件清单、阅读顺序、体积预算、核验口径 | ✅ |
| `10-modern-lane/00-scaffold.md` | 新建 NeoForge 1.21 工程 + 已有工程改造；`settings.gradle` / `build.gradle` / `gradle.properties` / 主类骨架全文 | ✅ 与 `example-mod` 对应 |
| `10-modern-lane/10-blocks-items.md` | 方块 / 物品 / 配方 / 标签 / 战利品 / 数据生成（**注册写法正文**） | ✅ P1 已编译验证 |
| `10-modern-lane/15-extra-registries.md` | 附魔 / 药水 / 状态效果 / 声音 / 属性 | ✅（非测试内容标「参考未验证 / v0.2 排期」） |
| **`10-modern-lane/70-version-matrix.md`** | **版本差异矩阵**：1.21.x ↔ 1.20.1 逐行对照（`DeferredRegister`、Data Components、配方 `result` 字段、`pack_format` 34↔15、`ResourceLocation` API、标签/战利品目录单复数、工具链版本）。**换版本前必读** | ✅ |
| `10-modern-lane/80-loader-patterns.md` | 同版本的 NeoForge / Fabric / Forge 等价写法 | ✅（Fabric/Forge 行未验证） |
| `10-modern-lane/90-troubleshooting.md` | 现代线故障速查：Mixin 注入失败、blockstate 缺失（黑紫方块）、`NoClassDefFoundError` | ✅ P0 最小版 |
| `20-legacy-1122-lane/*` | 1.12.2 线（`00-scaffold` / `10-blocks-items` / `90-troubleshooting` / `README`） | ✅ 参考用 |
| **未创建（P2 排期）** | `10-modern-lane/20-machines.md`（机器/JEI）、`30-worldgen.md`、**`40-entities.md`**、**`50-client.md`**、**`60-networking.md`**、`85-testing-ci.md`、`references/05-porting.md` | ❌ |

> **对本项目的直接影响**：我们的核心恰是**实体 + 客户端渲染 + 网络**，而这三篇技能文档**都不存在**。所以这三块要我们自己趟通，并**回写**成技能文档（见 [PORT-PLAN.md](PORT-PLAN.md) 第四节拦路虎 4）。

### 4.2 原模组源码解压位置（只许查玩法事实 / 名录）

| 路径 | 内容 |
|---|---|
| `E:\titanmod\.tmp\titans-src\The-Titans-Mod-main\` | 本体源码（1.12.2），顶层为 `.github/` `gradle/` `src/` `README.md` `build.gradle` |
| `E:\titanmod\.tmp\mac-src\Minecraft-Adventures-Core-master\` | 依赖库 MrbtUtil 3.0 |
| `E:\titanmod\.tmp\titans.zip` / `mac-*.zip` | 上述两份的原始 zip |
| `E:\titanmod\.tmp\legal-research\` | 合规调研产物：Modrinth 项目 JSON（`proj-*.json`、`search-titans.json`）、CurseForge widget JSON、GitHub issues 快照 |

**🚫 使用限制（红线）**：`titans-src/` 下 **`.java` 文件禁止阅读**。允许用途只有两条：

1. 核实「发行版有哪些内容」——**看资源文件名清单**（`src/main/resources/`），不看代码逻辑；
2. 避免重名——比对物品 / 实体名清单。

参考：[07-compliance.md](07-compliance.md) 第四节（净室流程）与第三节 3.3（禁用名表）。

### 4.3 本工程内部依据

| 文件 | 什么时候看 |
|---|---|
| [00-overview.md](00-overview.md) | 确认边界、命名、差异化五条骨架 |
| [07-compliance.md](07-compliance.md) | 任何「能不能用这份资料」的疑问，**一律以它为准** |
| [20-environment.md](20-environment.md) | 环境变量、命令、构建报错、网络通道 |
| [PORT-PLAN.md](PORT-PLAN.md) | 历史存档：为什么不做代码迁移（含竞品情报与工作量量级） |
| 仓库根 `ENVIRONMENT.md` | 环境原始速查（由别人维护，本索引不改它） |

---

## 五、链接维护规则

1. **新增链接必须同时写三段**：URL、**它能提供什么**（一句话）、**可达性标记**。
2. 可达性标记必须来自**本次实测**，不能凭印象；测不了就写「未验证 ❓」，不要写成「应该可以」。
3. 实测日期变化超过一个季度时，重新跑一遍可达性（脚本见 [20-environment.md](20-environment.md) 6.3）。
4. **涉及原模组与竞品的链接，必须同时带上合规标注**（见第一节与第二节），否则视为文档缺陷。
5. 平台 API（Modrinth）返回的数据会变（下载量、版本列表），引用时**写明抓取日期**，不要写成长期事实。
