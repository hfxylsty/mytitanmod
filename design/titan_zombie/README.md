# 僵尸泰坦 —— Blockbench 工作目录

> 本目录是**给人编辑的工作区**。游戏实际读取的文件在模组资源目录里（见下）。

## 一、这里的文件是什么

| 文件 | 用途 |
|---|---|
| `titan_zombie.geo.json` | **模型**（Bedrock 几何格式）。这是**游戏实际读取的格式** |
| `titan_zombie.png` | **贴图**（256×256）。原版僵尸皮肤放大 4 倍；右下角 `(232,232)` 处放入了原版铁剑的 16×16 贴图 |
| `titan_zombie.animation.json` | **动画**（idle / walk / attack 三个） |

这些都是**当前游戏正在用的版本**（原版僵尸 ×16，手臂前伸），可直接在 Blockbench 里打开编辑。

## 二、json 和 bbmodel 的区别（重要）

| | `.geo.json` | `.bbmodel` |
|---|---|---|
| 是什么 | **导出格式**，GeckoLib 在游戏里读它 | **Blockbench 工程文件**，可反复编辑 |
| 能否直接编辑 | 能，但不好用（纯数据） | 能，有完整编辑器状态 |
| 游戏读哪个 | ✅ **只读这个** | ❌ 不读 |

**正确工作流**：

```
打开 .geo.json  →  另存为 .bbmodel（工程）  →  编辑  →  导出覆盖 .geo.json
```

⚠️ **只在 Blockbench 里改，不要手写 json。**
`.bbmodel` 是"源文件"，`.geo.json` 是"编译产物"。建议改完后把 `.bbmodel` 也存到本目录，方便下次继续改。

## 三、在 Blockbench 里怎么操作

### 1. 打开模型

```
File → Open Model
选择：E:\titanmod\design\titan_zombie\titan_zombie.geo.json
```

如果提示加载贴图，选同目录的 `titan_zombie.png`。

### 2. 立刻另存为工程文件（强烈建议）

```
File → Save Project As
保存为：E:\titanmod\design\titan_zombie\titan_zombie.bbmodel
```

这样以后随时可以重新打开继续改，不用再从 json 反推。

### 3. 加铁剑

1. 右侧 **Outliner** 选中 **`right_arm`** 骨骼
2. 点 **Add Cube**，在左侧属性面板填：

   | 属性 | 值 |
   |---|---|
   | Position X | `-104` |
   | Position Y | `224` |
   | Position Z | `-1` |
   | Size X | `16` |
   | Size Y | `256` |
   | Size Z | `2` |

3. 切到 **UV** 面板，把 6 个面的 UV 拖到贴图右下角 **`(232,232)` 起 `16×16`** 的区域
4. 用鼠标在 3D 视图里拖动，调到"握在右手里"的位置

### 4. 导出

```
File → Export → Export Bedrock Geometry
覆盖保存到：
E:\titanmod\titanreborn\src\main\resources\assets\titanreborn\geo\entity\titan_zombie.geo.json
```

贴图没改就不用重新导出。

## 四、硬性约束

| 约束 | 原因 |
|---|---|
| **贴图必须保持 256×256** | 改了会导致全身 UV 偏移 2 倍、模型糊成一团（已踩过这个坑） |
| **骨骼名不能改** | 代码和动画按名字找骨骼，特别是 `head`、`right_arm`、`left_arm`、`right_leg`、`left_leg`、`torso` |
| **不能删 `right_arm_lower` / `right_leg_lower` / `left_leg_lower`** | 动画文件引用了这些名字。它们没有立方体，是空占位骨骼，删掉会导致动画解析失败 |
| **动画名不能改** | `animation.titan_zombie.idle` / `.walk` / `.attack`，代码里写死了 |
| **导出格式选 Bedrock Geometry** | 不是 Java 模型 |

## 五、关键坐标（模型空间，单位=像素，1 格 = 16 像素）

| 部位 | 位置 |
|---|---|
| 模型总高 | 512 像素 = 32 格 |
| 模型总宽 | 256 像素 = 16 格 |
| 头 | y 384 ~ 512 |
| 躯干 | y 192 ~ 384 |
| 右手骨骼 pivot | `(-96, 384, 0)`（肩部） |
| 右臂立方体 | origin `(-128, 224, -32)` size `(64, 160, 64)` |
| **手掌中心** | **`(-96, 224, 0)`** ← 剑应该握在这里 |
| 左臂立方体 | origin `(64, 224, -32)` size `(64, 160, 64)` |
| 腿 | y 0 ~ 192 |

## 六、游戏实际读取的位置

编辑完导出到：

```
titanreborn/src/main/resources/assets/titanreborn/
  geo/entity/titan_zombie.geo.json
  textures/entity/titan_zombie.png
  animations/entity/titan_zombie.animation.json
```

改完告诉我，我重新构建并启动验证。
