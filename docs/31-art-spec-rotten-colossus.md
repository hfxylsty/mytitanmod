# 31 · 腐骸泰坦 美术制作规格（给 Blockbench 用）

> 本文件职责：给做模型/贴图/动画的人（或你自己用 Blockbench）一份**精确规格**，
> 保证产出能被代码直接接上，不需要改 Java。
>
> 目标文件：`assets/titanreborn/geo/entity/rotten_colossus.geo.json`
> 　　　　　`assets/titanreborn/animations/entity/rotten_colossus.animation.json`
> 　　　　　`assets/titanreborn/textures/entity/rotten_colossus.png`

## 〇、为什么上一版模型完全不对（避免重犯）

上一版是我用程序生成的，有三个硬错误，**请勿参照**：

| 错误 | 说明 |
|---|---|
| `cubes.origin` 坐标系用错 | Bedrock 规范里 `origin` 是**模型空间绝对坐标**，我写成了「相对 pivot 的偏移」，导致所有部件叠在一起并沉到地面以下 |
| UV 全部是 `[0,0]` | 9 个部件共用贴图同一块区域，脸和腿长得一模一样 |
| 没有骨骼父子关系 | 挥臂动画不会带动手臂，因为没有 `parent` 层级 |

## 一、Blockbench 设置

| 项 | 值 |
|---|---|
| 新建项目类型 | **Bedrock/基岩版模型**（不是 Java 方块/物品模型） |
| 模型类型 | **Entity（实体）** |
| 格式版本 | `1.12.0` |
| 模型标识符 | `geometry.rotten_colossus` |
| 贴图尺寸 | 128 × 128（也可以先用 Blockbench 自动分配，导出时告诉我实际尺寸） |
| 坐标单位 | 像素，**1 格 = 16 像素** |

> ⚠️ 必须是 **Bedrock 实体模型**。用 Java 模型（`models/` 下那种）导出是不行的——
> GeckoLib 只认 Bedrock 几何格式。

## 二、骨骼名（**这是硬约束，必须逐字一致**）

| 骨骼名 | 用途 | 是否必须 |
|---|---|---|
| `body` | 躯干（根骨骼） | ✅ 必须 |
| `head` | 头 | ✅ 必须（GeckoLib 靠它做头部朝向） |
| `right_arm` | 右臂 | ✅ 动画用 |
| `left_arm` | 左臂 | ✅ 动画用 |
| `right_leg` | 右腿 | ✅ 动画用 |
| `left_leg` | 左腿 | ✅ 动画用 |
| **`bone_plate_0`** | 胸前第 1 片腐骨甲片 | ✅ **必须，名字一字不能差** |
| **`bone_plate_1`** | 胸前第 2 片腐骨甲片 | ✅ **必须** |
| **`bone_plate_2`** | 胸前第 3 片腐骨甲片 | ✅ **必须** |

**为什么甲片名字是硬约束**：破防系统的逻辑是「打中这 3 个具名部件 → 破防」。
代码里写死了这三个名字（`TitanZombie` 里的 `bone_plates`），
名字不一致就会出现「看得见甲片、打不掉」的问题。

甲片的**位置和形状你可以自由设计**，只要：
- 三个部件是**独立骨骼**（不是躯干的一部分）
- 视觉上在泰坦**正面胸口**，玩家能瞄准
- 每个甲片**不要太小**（建议不小于 10×10 像素的投影面积，否则打不中）

## 三、骨骼层级（`parent` 关系）

```
body（根）
├── head          parent = body
├── right_arm     parent = body
├── left_arm      parent = body
├── bone_plate_0  parent = body
├── bone_plate_1  parent = body
└── bone_plate_2  parent = body

（另建两个独立根骨骼）
right_leg         无 parent
left_leg          无 parent
```

> 说明：腿不挂在 body 下，是为了让「躯干旋转」不影响腿部着地。
> 如果你有更好的绑定方式，可以调整，只要动画看起来正常。

## 四、尺寸与比例（**必须与碰撞箱对齐**）

碰撞箱已经定死在代码里：`EntityType.Builder.sized(4.0F, 12.0F)`，
也就是 **宽 4 格 × 高 12 格**。换算成像素：

| 项 | 像素值 |
|---|---|
| 总高 | 约 **190 像素**（≈ 11.9 格） |
| 总宽 | ≤ **64 像素**（≈ 4 格） |

参考值（可以微调，但**总高别超 200 像素**，否则会穿模/打不中）：

| 部件 | 建议尺寸（宽 × 高 × 厚） | 建议位置（Y 范围，像素） |
|---|---|---|
| 头 | 40 × 40 × 32 | 136 ~ 176 |
| 躯干 | 64 × 65 × 40 | 65 ~ 130 |
| 单臂 | 20 × 70 × 24 | 60 ~ 130（挂点在肩，Y≈130） |
| 单腿 | 28 × 65 × 28 | 0 ~ 65 |

> **对齐很重要**：模型总高和碰撞箱（12 格）差太多，会出现「看着打中了却打不到」
> 或「明明离得远却被打中」。做完后可以进游戏打一下验证。

## 五、动画（**三个动画名必须逐字一致**）

在 Blockbench 的 **Animate** 标签页创建，导出为 Bedrock 动画格式（`format_version: 1.8.0`）：

| 动画名（必须逐字一致） | 循环 | 用途 | 最低要求 |
|---|---|---|---|
| `animation.rotten_colossus.idle` | 循环 | 待机 | 躯干/头部轻微起伏即可 |
| `animation.rotten_colossus.walk` | 循环 | 行走 | 双腿前后摆、双臂反向摆 |
| `animation.rotten_colossus.attack` | **不循环** | 攻击（一次） | 手臂大幅挥下 |

动画长度自定（建议 idle 4 秒、walk 1 秒、attack 0.75 秒）。

> 代码已按这三个名字注册：idle/walk 自动按「是否移动」切换，
> attack 由战斗逻辑用 `triggerAnim` 触发。名字不一致就会「动画不播」。

## 六、贴图

- 128 × 128 PNG（若你改了尺寸，导出时告诉我，我去改 geo 里的 `texture_width/height`）
- UV 用 Blockbench 自动展开即可，**只要每个部件不重叠**
- 风格建议：腐绿/骨白配色；甲片明显区别于躯干（玩家要能一眼看出该打哪）
- **合规**：贴图必须自绘或用明确授权素材，不得使用原模组《泰坦生物》的贴图（见 [07-compliance.md](07-compliance.md)）

## 七、导出与交付

在 Blockbench 里 **File → Export**：

| 导出项 | 得到文件 | 放到哪里 |
|---|---|---|
| Export Bedrock Geometry | `.geo.json` | `titanreborn/src/main/resources/assets/titanreborn/geo/entity/rotten_colossus.geo.json` |
| Export Animations | `.animation.json` | `titanreborn/src/main/resources/assets/titanreborn/animations/entity/rotten_colossus.animation.json` |
| Export Texture | `.png` | `titanreborn/src/main/resources/assets/titanreborn/textures/entity/rotten_colossus.png` |

**文件名必须完全一致**（`rotten_colossus`），因为 GeckoLib 按约定推导路径
（见 [04-client-rendering.md](04-client-rendering.md) §2.3）。

把文件放好或发给我，我来：
1. 替换资源文件；
2. `gradlew build` + `runClient` 验证能加载、无解析错误；
3. 如果是新泰坦（不是腐骸泰坦），顺手接好实体注册与渲染器。

## 八、自查清单（交付前过一遍）

- [ ] 是 **Bedrock 实体模型**（不是 Java 模型）
- [ ] 骨骼名逐字一致，特别是 **`bone_plate_0/1/2`** 三个
- [ ] 三个甲片是**独立骨骼**且能被打中（在正面胸口、不太小）
- [ ] 总高约 190 像素（≈ 12 格），与碰撞箱对齐
- [ ] 三个动画名逐字一致，attack 不循环
- [ ] 贴图 UV 无重叠，甲片颜色明显区别于躯干
- [ ] 文件名都是 `rotten_colossus`
- [ ] 贴图是自绘/授权素材，非原模组素材
