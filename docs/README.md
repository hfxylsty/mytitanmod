# 文档体系

本目录是本工程的**唯一权威信息来源**。代码可以重写，文档必须先对。

## 文档地图

| 编号 | 文件 | 职责 | 状态 |
|---|---|---|---|
| — | [README.md](README.md) | 本文件：文档地图与写作约定 | ✅ |
| 00 | [00-overview.md](00-overview.md) | 项目定位、目标与非目标、命名、边界 | ✅ |
| 01 | [01-requirements.md](01-requirements.md) | 需求来源、玩法还原清单、差异化需求 | ✅ |
| 02 | [02-architecture.md](02-architecture.md) | 工程与代码架构、模块划分、注册体系 | ✅ |
| 03 | [03-gameplay-systems.md](03-gameplay-systems.md) | 核心玩法系统设计（本项目重点） | ✅ |
| 04 | [04-client-rendering.md](04-client-rendering.md) | 客户端渲染与动画方案 | ✅ |
| 05 | [05-data-and-assets.md](05-data-and-assets.md) | 数据驱动层与资源规范 | ✅ |
| 06 | [06-roadmap.md](06-roadmap.md) | 里程碑、垂直切片、验收标准 | ✅ |
| 07 | [07-compliance.md](07-compliance.md) | 净室纪律、授权边界、发布红线 | ✅ |
| 09 | [09-glossary.md](09-glossary.md) | 术语表（中英对照） | ✅ |
| 10 | [10-content-roster.md](10-content-roster.md) | 内容清单（泰坦/召唤物/物品） | ✅ |
| 20 | [20-environment.md](20-environment.md) | 开发环境、工具链、已知坑 | ✅ |
| 30 | [30-m0-verification.md](30-m0-verification.md) | M0 技术验证的实测记录与结论 | ✅ |
| 31 | [31-art-spec-rotten-colossus.md](31-art-spec-rotten-colossus.md) | 腐骸泰坦美术制作规格（给 Blockbench） | ✅ |
| 90 | [90-references.md](90-references.md) | 外部资料与信息来源 | ✅ |
| — | [PORT-PLAN.md](PORT-PLAN.md) | 历史存档：最初（迁移路线）的方案分析 | 🗄️ 存档 |

> `PORT-PLAN.md` 记录的是"要不要迁移、怎么迁移"的决策过程。方向确定后（见 `00-overview.md`），它是历史依据，**不再更新**；现役方案以 00~07 号文档为准。

## 写作约定

1. **全中文**。专有名词、API 名、文件路径、命令、报错原文保持原样不翻译（首次出现时给中文解释）。
2. **事实与推测分开写**。推测必须显式标注「推测」；不确定的写「待验证」，不要写成既成事实。
3. **每条关键结论要有依据**。依据可以是：本地文件路径、上游 URL、工具输出。禁止凭印象写 API。
4. **面向 1.21.1 + NeoForge** 写具体写法，不写"大约这样"。
5. 涉及原模组时，**只描述玩法与结构，不粘贴其源码或资源**（见 `07-compliance.md`）。
6. 文档中的代码块必须是**我们自己的实现**，且能对上 NeoForge 21.1.x 的真实 API。

## 状态图例

- ✅ 已完成
- 🚧 编写中
- ⬜ 待写
- 🗄️ 存档（不再更新）

## 改文档的流程

1. 先读 `00-overview.md` 确认边界，再读要改的那一篇；
2. 架构类改动（`02`/`03`/`04`）必须同步检查 `06-roadmap.md` 的验收标准是否还成立；
3. 合规相关问题一律回到 `07-compliance.md`，不要在别处自行放宽。
