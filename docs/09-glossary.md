# 09 · 术语表（中英对照）

**本文件职责**：统一本项目全部文档与代码注释里的术语用法。任何文档/代码首次使用某个专有名词，都应能在本表查到确定的中英对照与一句话释义；查不到的，先在本表补齐再写别处（见 `README.md` 写作约定）。

**读者**：读玩法文档的新玩家（只需看 §1、§2 的「释义」列）与写代码的开发者（需看 §1、§2 的「本项目设计/实现落点」列、§3 全部）。

**口径**：适用 **Minecraft 1.21.1 + NeoForge 21.1.x**（当前验证工程用 NeoForge 21.1.252，见 [example-mod/build.gradle](<../example-mod/build.gradle>)）。表中「文件位置」列的路径均为相对路径，根是模组工程根目录。

> **合规提醒（先读）**：§1 收的是**原模组《泰坦生物》(The Titans) 的玩法术语**。玩法思想不受版权保护，可以借鉴并自由描述；但**具体的物品名、贴图名、语言文件文本、代码结构不能照搬**。凡本表中标注「我方不得沿用此名」的条目，仅为帮助你读懂公开资料页而收录，**不得**出现在 `titanreborn` 的任何注册名、语言键或资源文件名里。
>
> **合规裁决以 [07-compliance.md](07-compliance.md) 为准**（该文件的定位是红线文件，与其他文档冲突时以它为准）。该文件 §3.3 的禁用名清单涵盖**武器、道具、生物、地名、模式**五类，其中若干条目乍看是「通用玩法词」（如生长血清、噩梦模式），但**已被明确列为禁用名**——本表 §1 保留它们只为让你读懂旧资料，落地时必须换成我们自己的名字。

---

## 1. 原模组玩法术语

依据：[mcmod《泰坦生物》资料页](https://www.mcmod.cn/class/628.html) 与 `PORT-PLAN.md` 第一节「事实修正」。这些是**玩法事实**，不是我们已实现的功能。

### 1.1 实体与阵营

| 中文 | 英文 | 一句话释义 | 在本项目里的对应设计（若已定） |
|---|---|---|---|
| 泰坦 | Titan | 原模组 19 种巨型 BOSS 的统称，拥有动画的巨型原版怪物，血量与体型远超原版同类 | 我们的核心实体=**泰坦 BOSS**；数量刻意少于原作（`00-overview.md` 非目标：不复刻 19 泰坦） |
| 圣骑士 | Paladin | 五级召唤物中最高一级，泰坦的「先驱」：可飞行、免疫火焰与中毒、伤害抗性极强、有普通/特殊两种攻击模式可切换、可召唤下等仆从；死亡时若附近有对应泰坦会把「天赋精华」送还并治愈它 | **差异化目标 1** 的顶点单位；英文注册名保留 `paladin`（通用词），能力表见 [03-gameplay-systems.md](03-gameplay-systems.md) §3 |
| 忠诚者 | Loyalist | 最低级召唤物，只做一件事：主动攻击对应泰坦所攻击的目标 | 召唤物谱系第 1 级，AI 最简（继承主人目标）；**英文注册名 `devotee`**（自建词根） |
| 牧师 | Priest | 身穿紫色长袍的召唤物，能治愈附近的受伤盟友 | 谱系第 2 级，职责=治疗；**英文注册名 `chaplain`** |
| 狂热者 | Zealot | 身穿青色长袍，跑得更快、跳得更高、能快速闪避、承受更多伤害，可杀死比自己更大的实体 | 谱系第 3 级，职责=突进/机动；**英文注册名 `zealot`** |
| 主教 | Bishop | 身穿绿色长袍，能给敌方「审判之雷」效果、召唤下等仆从、免疫火焰 | 谱系第 4 级，职责=远程/减益；**英文注册名 `hierarch`** |
| 召唤物 | Minion | 泰坦的仆从统称（忠诚者/牧师/狂热者/主教/圣骑士），不是泰坦的子嗣而是崇拜者。默认生成上限：忠诚者 120、牧师 60、狂热者 30、主教 15、圣骑士 6、特殊召唤物 6 | **差异化目标 1「五级谱系」**；上限作为配置项默认值。**注意**：「忠诚者/牧师/狂热者/主教/圣骑士」这组中文称谓是**待裁定项**（见 §1.4） |
| 原型 | Proto | 泰坦作为「某类生物的巨型版」的基准形态（如僵尸泰坦之于僵尸）。仓库里 7 个泰坦原型实体即此含义 | 我们用同一思路：每个泰坦绑定一个原版生物作为原型，强化怪由原型派生 |
| 强化原版生物 | Enhanced Vanilla Mob | 原版生物被「泰坦化」的强化版，原模组共 61 种，**均为泰坦召唤物** | 召唤物的来源素材；不做 61 种全量（见 `01-requirements.md` 玩法还原清单） |

### 1.2 生成与成长

| 中文 | 英文 | 一句话释义 | 在本项目里的对应设计（若已定） |
|---|---|---|---|
| 自然生成 | Natural Spawn | 泰坦的主要生成方式，机制类似「自然结构生成」：按玩家已加载区块的数量抽签，原模组几率 1/2000~1/25000 | **纳入 M3**；实现方式待定（结构式生成 vs 刷怪事件），见 [01-requirements.md](01-requirements.md) §2.1 与 §6 开放问题 3 |
| 圣骑士转化 | Paladin Ascension | 第二种生成方式：圣骑士生物接触「空灵平面」的超自然力量后**变成**泰坦 | **可选**；需要时按「事件触发 + 生物替换」实现 |
| 泰坦之魂 | Titan Spirit | 会主动附身对应圣骑士生物、使其变成泰坦的存在；所有泰坦之魂都会尝试这么做 | **改名**：我们的对应物叫**泰坦残响（`titan_echo`）**——既是召唤祭坛的燃料，也是圣骑士死亡时送还给泰坦的「天赋精华」的物品形态（[10-content-roster.md](10-content-roster.md)） |
| 生长血清 | Growth Serum | 第四种生成方式，也是唯一玩家可控的：向生物投放血清使其进化为泰坦 | **机制纳入，但名字禁用**：改用内容组已定的原创名**催生灵液（`quickening_ichor`）**，见 [10-content-roster.md](10-content-roster.md) |
| 重生长 | Regrowth | 泰坦血量低于 5% 时每 tick 有 1/1000 几率触发的「最后一招」：缩小到普通生物大小再重新长大，**重置血量**，期间免疫外来伤害。两只泰坦互斗时若一方重生长且另一方血量低于 50%，另一方也会跟进 | **差异化目标 2**；触发概率与免疫时长进配置；中文名「重生长」为通用描述词，可继续使用 |
| 噩梦模式 / 灭神模式 | Nightmare Mode / Godslayer(Deicide) Mode | 原模组的两个额外模式；噩梦模式下泰坦伤害上限由 1000 降为 500 | **机制纳入「双模式」，但这些名字全被禁用**（[07-compliance.md](07-compliance.md) §3.3 的模式类禁用名）：我们的困难模式**必须另起中文名与注册名**，命名未定前一律写作「困难模式（暂名）」。灭神模式**不做** |
| 大乱斗版本 | Mob Battles | 原模组的一个平行发行版，唯一区别是泰坦会不分敌我自相残杀 | **不做**（属发行变体，不是机制） |

### 1.3 战斗规则

| 中文 | 英文 | 一句话释义 | 在本项目里的对应设计（若已定） |
|---|---|---|---|
| 破防 / 击晕 | Break / Stun | 多数泰坦必须先被「特定方式击晕」才能有效受伤（如用特定弓射击、击落若干飞行水晶）；未击晕时处于**非破防状态**，免疫大部分外来伤害 | **差异化目标 3**；每个泰坦配一个可判定的弱点条件 |
| 伤害上限 | Damage Cap | 单次对泰坦的伤害上限 1000 点，困难模式（原「噩梦模式」）500 点；并伴随 1.5 秒伤害冷却 | **差异化目标 5**；用 `LivingIncomingDamageEvent#setAmount` + `#setInvulnerabilityTicks` 落地 |
| 窃魂伤害 | Soul Steal Damage | 泰坦攻击附带的伤害类型：无视护甲（对高度 ≥6 格的非泰坦目标同样无视），随难度缩放（简单减半、困难翻倍），强度与主攻击相同 | **差异化目标 5**；作为自定义 `DamageType` + 独立结算通道（落地细节见 [05-data-and-assets.md](05-data-and-assets.md) §5） |
| 虚空伤害 | Void Damage | 泰坦攻击附带的另一类伤害，与窃魂伤害同属「不可常规免疫」的一类；和平模式下额外附加多次伤害 | **差异化目标 5**；同样做自定义 `DamageType` |
| 减伤规则 | Damage Negation | 泰坦无视 <20 点伤害；免疫燃烧/窒息/摔落/虚空；免疫 `.setHealth`；免疫 `/kill`、WorldEdit butcher、秒杀类武器；流体中下沉但不会被淹死；虚空上可行走 | **差异化目标 5** 的主体，M1 起逐条实现 |
| 击退抗性 | Knockback Resistance | 所有泰坦 1 点击退抗性（=完全免疫击退） | 属性层直接设 `Attributes.KNOCKBACK_RESISTANCE = 1.0` |
| 泰坦护甲层级 | Titan Armor Tier | 原模组的自定义护甲档：小型 21 / 普通 22 / 大型 23 / 神 24（非原版 `Attributes.ARMOR` 的语义） | 用**自定义属性**表达，不占用原版护甲值（见 §2「属性」） |
| 仇恨连锁 | Aggro Chain | 类似僵尸猪人的仇恨传播：一只泰坦被打，附近同类一起进入敌对 | **纳入 M2**，广播半径与数量进配置 |
| 区块刷新重置 | Chunk Reload Reset | 泰坦因区块刷新而重置血量与仇恨；玩家退出重进同样重置 | **纳入 M2**；同时是避免「巨型实体存档体积膨胀」的手段，见 `01-requirements.md` §5 |

### 1.4 原模组专属名词（**我方不得沿用此名**）

以下条目仅用于帮助你读懂公开资料页与玩家教程。**这些是原作者的表达，我们一律另起名**（语言键、注册名、贴图名、音效名都不得相同或近似）：

| 原模组名称（仅供理解） | 类别 | 合规结论 |
|---|---|---|
| 创世之刃 / 完美之斧 | 武器的中文名 | **我方不得沿用此名**（[07-compliance.md](07-compliance.md) §3.3）；我们自建武器另有原创名，见 [10-content-roster.md](10-content-roster.md) 的武器线 |
| 末晶弓（哈肯典姆弓） | 破防用弓 | **我方不得沿用此名**（同上）；破防条件的设计思路可借鉴，物品名与贴图自建 |
| 生长血清 | 道具 | **我方不得沿用此名**；改用**催生灵液（`quickening_ichor`）** |
| 凋灵斯拉 / 执行之龙 / 欧米伽虫 / 末影巨人 / 终极铁傀儡泰坦 / 雪傀儡泰坦 | BOSS 名 | **我方不得沿用此名**；只借鉴「多阶段 BOSS」这一结构 |
| 噩梦模式 / 灭神模式 | 模式名 | **我方不得沿用此名**；我们的困难模式必须另起名 |
| 虚空之地 / 泰坦圣堂 | 地名 | **我方不得沿用此名**；维度与结构全部另起名 |
| 飞行末影水晶（作为破防目标） | 机制载体 | 机制思路可用；实体名与贴图自建 |
| 「Quit flailing that giant sword/axe at me」等台词 | 文本表达 | **禁止任何形式的复制或改写沿用**；我们的台词全部自写 |

**五项待裁定项（需要人拍板，不要自行沿用）**：

| # | 待裁定 | 现状 | 建议 |
|---|---|---|---|
| 1 | 五级中文称谓「忠诚者 / 牧师 / 狂热者 / 主教 / 圣骑士」 | 单个词是通用宗教等级词，但**这组组合与原模组官方中文译名一致**；英文注册名已改为自建词根（`devotee` / `chaplain` / `zealot` / `hierarch` / `paladin`） | 中文显示名也自建（如「信徒/司祭/狂信者/主祭/圣殿骑士」），彻底避开 |
| 2 | 「泰坦」一词本身 | 希腊神话通用词 | 可继续使用 |
| 3 | 困难模式的中文名与注册名 | 待定 | 命名确定后回填本表与 [03-gameplay-systems.md](03-gameplay-systems.md) |
| 4 | 玩法机制高度相似的整体风险 | 机制属「思想」，但若物品表/数值/地图都雷同，整体表达风险上升 | 靠差异化设计主动拉开距离 |
| 5 | 是否向原作者请求书面授权 | 未做 | 建议做（能消除全部疑虑并在 README 里光明正大致敬） |

---

## 2. Minecraft / NeoForge 技术术语

形态与文件位置按 **1.21.1** 写（1.20.1 的差异见技能文档 `references/10-modern-lane/70-version-matrix.md`，仅作对照）。

| 中文 | 英文 | 释义 + 1.21.1 里的具体形态 / 文件位置 |
|---|---|---|
| 模组 ID | `modid` | 模组的唯一命名空间，必须 ASCII 小写。本项目 = `titanreborn`（**禁用** `thetitans`，见 `00-overview.md`）。它出现在 `src/main/resources/META-INF/neoforge.mods.toml` 的 `modId`、Java 主类 `@Mod(...)`、以及所有 `assets/<modid>/`、`data/<modid>/` 目录名 |
| 注册表 | `Registry` | 游戏登记一切内容的中央清单。1.21.1 通过 `net.minecraft.core.registries.Registries` 取键（如 `Registries.ENTITY_TYPE`、`Registries.ITEM`、`Registries.ATTRIBUTE`、`Registries.MOB_EFFECT`） |
| 延迟注册 | `DeferredRegister` | NeoForge 的注册 API：先声明「注册表 + 名字 + 工厂」，加载时统一提交。工厂方法：`DeferredRegister.createBlocks(id)` / `createItems(id)` / `create(Registries.X, id)` / `createDataComponents(id)`；必须在主类构造器里 `register(bus)` 才生效（见 [example-mod/src/main/java/com/titanmod/TitanMod.java](<../example-mod/src/main/java/com/titanmod/TitanMod.java>)） |
| 数据组件 | `Data Component` | 1.20.5+ 取代旧 NBT 的物品附加数据体系：`Item.Properties().component(...)`、`itemStack.set(DataComponents.X, ...)`；自定义组件用 `DeferredRegister.createDataComponents(modid)` 注册 `DataComponentType` |
| 实体 | `Entity` / `LivingEntity` | 世界中的活动对象。1.21.1 注册用 `DeferredRegister.create(Registries.ENTITY_TYPE, id)` + `EntityType.Builder`；泰坦应继承 `Monster` 或 `PathfinderMob` |
| 实体属性 | `Attribute` / `AttributeSupplier` | 生物的数值属性（最大生命、攻击力、移动速度、击退抗性…）。自定义属性注册用 `Registries.ATTRIBUTE` + `RangedAttribute`（`Attribute` 构造器是 protected）；把属性表挂到实体类型用**模组总线事件** `EntityAttributeCreationEvent#put`（签名已核验：`E:\titanmod\.tmp\api-check\net\neoforged\neoforge\event\entity\EntityAttributeCreationEvent.java`） |
| 方块实体 | `BlockEntity` | 挂在方块上的「数据盒子」，让方块能存数据、跑逻辑。泰坦战利品箱、祭坛一类方块用 `DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, id)` |
| 实体渲染器 | `EntityRenderer` | 负责把实体画到屏幕上的客户端类。注册用模组总线事件 `EntityRenderersEvent.RegisterRenderers#registerEntityRenderer`（已核验：`E:\titanmod\.tmp\api-check\net\neoforged\neoforge\client\event\EntityRenderersEvent.java`） |
| 模型部件 | `ModelPart` | 模型树上的一个可独立旋转/平移的部件（头、臂、腿），属**渲染层**概念。注意与碰撞层区分：1.21.1 里「可分别命中的部位」由 **NeoForge 的 `PartEntity` + `IEntityExtension#isMultipartEntity()/#getParts()`** 承载（不是原版 `EntityPart`；原版末影龙用的是内部 `EnderDragonPart`）。方案与已核验的三个 API 见 [04-client-rendering.md](04-client-rendering.md) §3 |
| 动画 | `Animation` | 让模型部件按时间轴运动的数据。原版有 `AnimationDefinition`/关键帧机制（`client.animation` 包），泰坦的大幅攻击动作是否够用**待验证**；备选是第三方动画库或自建关键帧播放器——方案见 [04-client-rendering.md](04-client-rendering.md) |
| 数据驱动 | Data-driven | 把内容定义放进 JSON 而非 Java。1.21.1 里附魔（`data/<ns>/enchantment/`）、配方、战利品表、标签、世界生成全部数据驱动；本项目「Java 只写逻辑」的落点 |
| 战利品表 | `Loot Table` | 定义「击杀生物/破坏方块掉落什么」的 JSON。1.21.1 目录为**单数** `data/<modid>/loot_table/`（含 `blocks/`、`entities/` 子目录；1.20.1 是复数 `loot_tables/`） |
| 标签 | `Tag` | 给方块/物品/实体分组，供配方与规则按组引用。1.21.1 目录为单数 `data/<modid>/tags/item/`、`tags/block/`、`tags/entity_type/`（1.20.1 为复数 `items/`、`blocks/`） |
| 配方 | `Recipe` | 「怎么加工出什么」的 JSON，如 `data/<modid>/recipe/xxx.json`。1.21.1 的结果字段是 `"result": { "id": "...", "count": 1 }`（不再用 `item`） |
| 维度 | `Dimension` | 独立世界（原版下界/末地）。1.21.1 里维度是**数据驱动**：`data/<modid>/dimension/<name>.json`（引用一个维度类型）+ `data/<modid>/dimension_type/<name>.json`（定义高度、坐标缩放、天空/雾等）。**待验证**：JSON 的字段集需以 1.21.1 原版 `minecraft:the_nether` 数据文件为准逐字段核对 |
| 生物群系 | `Biome` | 维度内的气候/植被分区。1.21.1 为数据驱动 `data/<modid>/worldgen/biome/<name>.json`；泰坦维度需要的群系在此定义 |
| 载荷 / 自定义包 | `Payload` / `CustomPacketPayload` | 客户端↔服务端传自定义数据的机制。1.21.1 用 `RegisterPayloadHandlersEvent` 拿 `PayloadRegistrar`，`playBidirectional(TYPE, STREAM_CODEC, handler)` 注册；发送用 `PacketDistributor.sendToPlayer/sendToAllPlayers/sendToServer`。注意限额：发往客户端的包 ≤1 MiB，发往服务端的包 <32 KiB（依据：[NeoForge 1.21.1 官方文档 · Registering Payloads](https://docs.neoforged.net/docs/1.21.1/networking/payload/)） |
| 访问转换器 | `Access Transformer`（AT） | 声明式放宽/改名原版类成员的可见性，文件如 `src/main/resources/META-INF/accesstransformer.cfg`，在 `build.gradle` 的 `neoForge { accessTransformers { ... } }` 里登记。**原则**：只在没有事件可用时使用，用了必须在代码注释里写明理由 |
| Mixin | `Mixin` | 运行时织入、改写原版类的技术（`titanreborn.mixins.json`）。本项目**默认禁用**，除兼容性补丁外不动原版内部（理由：`PORT-PLAN.md` 拦路虎 2 —— 原模组对原版内部的强耦合正是它无法跨版本的原因） |
| 映射 | `Mappings` | 把混淆名还原为可读名的对照表。本项目用 **Mojang 官方映射（Mojmap，`official` 频道）**；`ResourceLocation` 在 1.21.1 必须用 `ResourceLocation.fromNamespaceAndPath(ns, path)` 或 `ResourceLocation.parse("ns:path")`，`new ResourceLocation(...)` 已不可用 |
| Parchment | `Parchment` | 在 Mojmap 之上补充**参数名与 Javadoc** 的社区映射层，通过 ModDevGradle 的 `neoForge { parchment { ... } }` 启用。它不改变方法名，只提升 IDE 可读性；**可选**，不影响产物 |
| GameTest | `GameTest` | 游戏内自动化测试框架：`.nbt` 结构模板 + `@GameTest` 方法（`Consumer<GameTestHelper>`），用 `@GameTestHolder(MODID)` 注册，模板放 `data/<namespace>/structure/`，可用 `gradlew runGameTestServer` 批量跑并让退出码=失败数（依据：[NeoForge 1.21.1 官方文档 · Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)） |

**本项目会用到的关键事件（已在本机核验签名）**：

| 事件 | 用途 | 核验来源 |
|---|---|---|
| `EntityAttributeCreationEvent` | 给泰坦挂属性表 | `E:\titanmod\.tmp\api-check\net\neoforged\neoforge\event\entity\EntityAttributeCreationEvent.java`（`#put(EntityType, AttributeSupplier)`） |
| `EntityRenderersEvent.RegisterRenderers` | 注册泰坦渲染器 | `E:\titanmod\.tmp\api-check\net\neoforged\neoforge\client\event\EntityRenderersEvent.java`（`#registerEntityRenderer`） |
| `LivingIncomingDamageEvent` | 伤害上限 / 冷却 / 免疫规则 | `E:\titanmod\.tmp\api-check\net\neoforged\neoforge\event\entity\living\LivingIncomingDamageEvent.java`（`#setAmount`、`#setInvulnerabilityTicks`、`#addReductionModifier`） |

---

## 3. 工程术语

| 中文 | 英文 | 一句话释义 | 本项目落点 |
|---|---|---|---|
| 净室重实现 | Clean-room Reimplementation | 只依据公开的**玩法描述**（资料页、玩家教程）从零写实现，不阅读、不搬运、不反编译对方的源码与资源 | 全项目最高纪律，见 [07-compliance.md](07-compliance.md) §4 与 `PORT-PLAN.md` 第四节 |
| 玩法思想 / 表达 | Idea / Expression | 法律上的二分：机制、数值设计、系统结构属「思想」可借鉴；源码、贴图、音效、模型、语言文本属「表达」不可复制 | 判定任何「能不能抄」的最终标尺，见 [07-compliance.md](07-compliance.md) §2 |
| 垂直切片 | Vertical Slice | 一条端到端打通的最小链路：1 个泰坦 + 1 级召唤物 + 破防 + 重生长 + 渲染 | M1，定义见 [06-roadmap.md](06-roadmap.md)，技术验证口径见 `PORT-PLAN.md` 第五节 |
| 里程碑 | Milestone | 有明确交付物与验收标准的阶段。本项目分 M1 垂直切片 / M2 核心玩法 / M3 内容扩展与打磨 | 权威定义见 [06-roadmap.md](06-roadmap.md)（与 `PORT-PLAN.md` 第六节的「阶段 1~4」对应） |
| 验收标准 | Acceptance Criteria | 可自动判定或可复现人工判定的完成条件，禁止「看起来差不多」 | 每张任务卡与每个里程碑都必须带；`00-overview.md` 3.2 要求「可自动判定」，实例见 [06-roadmap.md](06-roadmap.md) 的 A1~A9 |
| 差异化 | Differentiation | 我们不与竞品拼内容数量的那部分：五级召唤物 / 重生长 / 破防 / 双模式 / 伤害规则体系 | `00-overview.md` 3.1「差异化目标」，即本项目的产品骨架 |

---

## 4. 维护规则

1. **先补表再写文档**：任何文档出现本表没有的专有名词，先在本表加一行，再回去写正文。
2. **中英对照固定**：同一个概念在所有文档里只用一种译名（例：Regrowth 一律译「重生长」，不用「再生」）。
3. **推测必须标注**：本表中的「待验证」与「推测」会在实测后改写；改的时候连同 `01-requirements.md` §6 的开放问题一起更新。
4. **合规条目只增不减**：§1.4 的「我方不得沿用此名」清单随发现新增，不得为了写文档方便而删减。
