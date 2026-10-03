# 05 · 数据驱动层与资源规范

> 本文件职责：定义 `data/` 与 `assets/` 到底放什么、写成什么格式、有哪些坑。
> **1.21.1 的关键变化：目录名用单数、模型格式变了、很多东西必须数据驱动。**

## 一、目录总览

```
src/main/resources/
├── META-INF/neoforge.mods.toml      # mod 元数据（modId 必须与 @Mod 一致）
├── pack.mcmeta                      # pack_format 34（1.21.1）
├── assets/titanreborn/              # 只影响外观与文本
│   ├── lang/
│   │   ├── en_us.json
│   │   └── zh_cn.json
│   ├── textures/
│   │   ├── block/
│   │   ├── item/
│   │   ├── entity/                  # 泰坦贴图（GeckoLib 用）
│   │   └── particle/
│   ├── models/
│   │   ├── block/
│   │   └── item/
│   ├── blockstates/
│   ├── geo/entity/                  # GeckoLib 的 Bedrock 几何模型
│   ├── animations/entity/           # GeckoLib 的动画文件
│   └── sounds.json
└── data/titanreborn/                # 定义游戏玩法
    ├── recipe/                      # 单数！
    ├── loot_table/                  # 单数！
    ├── tags/
    │   ├── item/                    # 单数！
    │   └── block/                   # 单数！
    ├── damage_type/                 # 窃魂/虚空伤害
    ├── dimension/                   # M3
    └── worldgen/biome/              # M3
```

## 二、⚠️ 最容易踩的坑：目录用单数

1.20.5 / 1.21 起，以下目录名从复数改成了**单数**。写错**不会报错，只是不生效**——最典型的静默失败：

| 内容 | 1.21.1 正确 | 旧写法（失效） |
|---|---|---|
| 配方 | `data/<ns>/recipe/` | ~~`recipes/`~~ |
| 战利品表 | `data/<ns>/loot_table/` | ~~`loot_tables/`~~ |
| 物品标签 | `data/<ns>/tags/item/` | ~~`tags/items/`~~ |
| 方块标签 | `data/<ns>/tags/block/` | ~~`tags/blocks/`~~ |

> 依据：技能文档 `references/10-modern-lane/70-version-matrix.md` 的差异矩阵（已核对 1.21.x 列）。

## 三、配方格式（1.21.1）

关键差异：`result` 用 **`id`** 字段，不是 `item`：

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["XXX", " Y ", " Y "],
  "key": { "X": { "item": "titanreborn:titan_bone" }, "Y": { "item": "minecraft:stick" } },
  "result": { "id": "titanreborn:bone_breaker", "count": 1 }
}
```

> 照 1.20.1 的教程写 `"result": { "item": ... }` 到 1.21 工程 → **配方不生效且不报错**。

## 四、战利品表与标签

- 掉落：`data/titanreborn/loot_table/entities/titan_zombie.json`，由实体在 `getLootTable()` 里引用；
- 标签：用于把我们的物品归入原版类别（如让锤子能被当镐用、让泰坦可被某类伤害影响）。

**泰坦的掉落表要有"保底"设计**：BOSS 级实体建议用 `rolls` + 权重，避免刷不出关键材料。

## 五、自定义伤害类型（窃魂 / 虚空）

1.21.1 的伤害类型是**数据驱动**的，放在 `data/titanreborn/damage_type/`：

```json
{
  "message_id": "soul_steal",
  "exhaustion": 0.0,
  "scaling": "when_caused_by_living_non_player"
}
```

配合 `data/minecraft/tags/damage_type/` 下的标签控制行为（例如 `bypasses_armor`、`bypasses_invulnerability`）。

> **待验证**：`bypasses_armor` 等标签的确切名称与语义，实现时对照原版 `data/minecraft/tags/damage_type/` 核对。

## 六、语言文件

用 JSON（不是旧的 `.lang`）：

```json
{
  "item.titanreborn.bone_breaker": "Bone Breaker",
  "entity.titanreborn.titan_zombie": "Zombie Titan",
  "itemGroup.titanreborn.main": "Titan Reborn"
}
```

**命名纪律**：物品名与实体名必须用 [10-content-roster.md](10-content-roster.md) 的原创命名，
**不得与原模组名称相同**（见 [07-compliance.md](07-compliance.md) 3.3 禁用清单）。

## 七、模型与贴图

### 7.1 两套模型体系，别搞混

| 用途 | 格式 | 位置 |
|---|---|---|
| 方块 / 物品 | Java 模型 JSON | `assets/<ns>/models/` |
| **泰坦实体** | **Bedrock 几何模型**（`.geo.json` + `.animation.json`） | `assets/<ns>/geo/`、`assets/<ns>/animations/` |

> 因为泰坦用 GeckoLib 做动画（见 [04-client-rendering.md](04-client-rendering.md)），
> 实体模型必须用 **Bedrock 格式**导出，**不能用 Java 方块模型的导出方式**。
> 这条要提前告知做美术的人，否则模型要返工。

### 7.2 贴图路径规范

- 方块贴图：`textures/block/xxx.png`（单数 `block`）
- 物品贴图：`textures/item/xxx.png`（单数 `item`）
- 实体贴图：`textures/entity/titan_zombie.png`

> 1.21 里 `textures/blocks/`、`textures/items/`（复数）**不再被识别**，表现为黑紫方块。

### 7.3 贴图规格（设计约定）

| 类型 | 建议尺寸 | 备注 |
|---|---|---|
| 方块 | 16×16 | 保持原版风格 |
| 物品 | 16×16 | |
| 泰坦实体 | 128×128 或 256×256 | 巨型实体建议高分辨率，但**注意显存** |
| 部件贴图 | 复用宿主贴图 | 避免额外贴图开销 |

## 八、音效

`sounds.json` + `sounds/` 下的 `.ogg`：

```json
{
  "entity.titan_reborn.titan_zombie.idle": {
    "category": "hostile",
    "sounds": ["titanreborn:entity/titan_zombie/idle1", "titanreborn:entity/titan_zombie/idle2"]
  }
}
```

**合规提醒**：音效必须自制或使用**明确授权**的素材（CC0 音效库等），**不得使用原模组音效**。

## 九、资源规模预算（M1）

| 类别 | M1 需要 | 说明 |
|---|---|---|
| 实体贴图 | 1 张（+ 可能 1 张部件贴图） | 僵尸泰坦 |
| 物品贴图 | 3~5 张 | 召唤道具、材料、1 件武器 |
| 方块贴图 | 1~2 张 | 矿石/祭坛 |
| 模型 | 1 个 `.geo.json` + 1 个 `.animation.json` | |
| 音效 | 3~4 个 | 待机、受伤、攻击、死亡 |
| 语言条目 | 15~25 条 | 中英各一份 |
| JSON（数据） | 5~8 份 | 配方、掉落、标签、伤害类型 |

> **先占位，后美术**：M1 阶段贴图可以用纯色块，只要模型与碰撞箱对得上就能验证玩法。
> 把美术留到 M2，避免早期被素材卡住。

## 十、核验依据

- 目录单复数、配方 `result.id`、贴图路径：技能文档 `E:\titanmod\.dsh\skills\minecraft-modding\references\10-modern-lane\70-version-matrix.md`
- `pack_format` 34（1.21.1）：同上的差异矩阵 + 已验证的 `example-mod/src/main/resources/pack.mcmeta`
- 伤害类型 JSON 字段：**待验证**（实现时对照原版 `data/minecraft/damage_type/` 与标签核对）
