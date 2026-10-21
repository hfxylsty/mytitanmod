---
name: titanreborn-dev
description: >
  《泰坦重生》(titanreborn) 项目的开发知识库。处理本项目特有的模型生成、GeckoLib
  骨骼与动画、Blockbench MCP 操作、坐标系约定与已知坑。Use when working on the
  titanreborn NeoForge mod — editing the titan zombie model/animation, using the
  Blockbench MCP bridge, or debugging "model/part not rendering" issues.
---

# 泰坦重生开发知识库

## 一、项目速览

| 项 | 值 |
|---|---|
| 模组 | `titanreborn`，Minecraft 1.21.1 + NeoForge 21.1.252 + GeckoLib 4.9.3 |
| 根目录 | `E:\titanmod`（git 仓库），模组工程 `E:\titanmod\titanreborn` |
| Java | `C:\dsh-tools\jdk-21.0.12.1+1` |
| 构建 | `GRADLE_USER_HOME=E:\titanmod\.gradle-home`，`gradlew.bat build\|runClient --no-daemon` |
| 资源目录 | `titanreborn/src/main/resources/assets/titanreborn/` |
| 设计目录 | `E:\titanmod\design\titan_zombie\`（Blockbench 工作区） |

**合规红线**：不得读取/搬运原模组 `.java` 源码。只能做行为与数值研究，
实现一律自写。禁用原名（创世之刃/完美之斧/末晶弓/生长血清等）。

## 二、模型与动画由 Java 工具生成，不手写 JSON

手写 JSON 会踩 PowerShell `ConvertTo-Json` 的**单元素数组降级成对象**坑，
导致 Blockbench/GeckoLib 解析失败。**一律用 Java + Gson 生成。**

存放在 `.tmp/mcp/` 的工具（不入库，需要时重新生成）：

| 工具 | 作用 |
|---|---|
| `BuildRig2.java` | 生成 15 骨骼 / 13 立方体骨架（v2 比例：腿:上半身≈1:1）|
| `SliceUv.java` | 按高度占比切分 UV，保证分段贴图连续 |
| `MakeWalk2.java` | 行走动画（腿部相位差 + 上身摇摆）|
| `MakeAttack.java` | 三段式攻击动画 |
| `MakeIdle.java` | 有机波形 idle（移植 pi-blockbench 范式）|
| `RemapAnim.java` | 骨骼改名时批量重映射动画通道 |
| `Post3.java` | 导出后处理：去重骨骼 + 修负 UV + 补 parent + 放大 bounds |

## 三、当前骨架（15 骨骼 / 13 立方体）

```
body(根, pivot 0,248,0, 无几何)
 └ hips (无几何，纯旋转支点)
    ├ spine1 → spine2 → chest        上半身 248~400（52/52/48）
    │           ├ neck → head        颈 400~416，头 416~512
    │           ├ upperarm_R → forearm_R   肩球关节 + 肘
    │           └ upperarm_L → forearm_L
    ├ thigh_R → shin_R               髋球关节 + 膝
    └ thigh_L → shin_L
```

**没有 hand / foot 骨骼**（用户明确不要）。

## 四、坐标系约定（血泪教训，务必遵守）

### 4.1 Bedrock JSON 与 Blockbench 编辑器符号相反

- 动画/立方体旋转在 **Blockbench 编辑器**里 `+80` = 手臂前伸
- 导出到 **Bedrock JSON** 变成 `-80`
- 写 JSON 时用 Bedrock 值：**手臂前伸 ≈ -80，举过头顶 ≈ -150，劈下 ≈ -25**

### 4.2 骨骼局部轴 ≠ 世界轴（不要靠矩阵推导）

手臂骨骼带前伸旋转，`translate` 作用在旋转后的坐标系里。
**实测结论**（用单轴大值 8.0 一眼看出方向，不要推导）：

| 平移参数 | 实际世界方向 |
|---|---|
| 第 1 个（局部 X）| 左右 |
| 第 2 个（局部 Y）| **上下**（正 = 下）|
| 第 3 个（局部 Z）| **前后** |

**方法论**：不确定轴方向时，固定其他轴为 0，给该轴一个明显大值（如 8.0），
让用户看一眼就能确定方向。比推导旋转矩阵快得多，也可靠得多。

### 4.3 模型正面是 -Z，背面是 +Z
（已用截图验证：从 -Z 能看到脸，从 +Z 只看到后脑勺）

## 五、GeckoLib 已知坑

| # | 坑 | 现象 | 解法 |
|---|---|---|---|
| 1 | **每个骨骼必须有 pivot** | 模型加载失败、`ArrayIndexOutOfBoundsException` in `arrayToVec` | 所有骨骼都写 pivot |
| 2 | **Blockbench 导出会丢 `parent`** | 子骨骼变游离、位置错乱 | 导出后用 `Post3.java` 补 parent |
| 3 | **导出会出现负数 `uv_size`** | 渲染异常/不显示 | 后处理取绝对值 |
| 4 | **导出会重复骨骼名** | 骨骼树冲突 | 后处理去重 |
| 5 | **骨骼改名是连锁的** | 渲染层写死的骨骼名字符串失配 → 附属物消失 | 全局 grep 骨骼名常量 |
| 6 | **`ItemDisplayContext` javac 解析不了** | 引用即编译失败（NeoForge 扩展枚举属性异常）| 用**反射**（运行时正常），见 `HandItemRenderer` |

## 六、手持武器：走原版装备系统，不要在 geo 里建模

**曾经失败的路**：往 `titan_zombie.geo.json` 加骨骼/立方体放剑。
GeckoLib 诊断日志显示骨骼和立方体都加载了、未隐藏，但就是不绘制
（换成 32×32 普通方块同样不显示）。多坑叠加，无法定位。

**正确方案**（用户提出的思路）：

```java
// 实体侧：原版装备槽
setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

// 渲染侧：GeckoLib 官方图层
addRenderLayer(new BlockAndItemGeoLayer<TitanZombie>(this) {
    getStackForBone → 指定骨骼返回主手物品
    renderStackForBone → 摆好位置后交给 HandItemRenderer
});
```

好处：剑是真正的原版物品，换武器/附魔/掉落/被拾取全部白送。

**关键细节**：

1. **必须挂在 `forearm_R`（前臂），不是 `upperarm_R`**
   —— 手在前臂末端。挂在上臂会导致**肘部一弯、剑就错位**。
2. 手掌偏移 = 前臂长度 = **4 格**（64 像素）
3. 必须先建立原版手部空间再渲染：
   ```java
   mulPose(Axis.XP.rotationDegrees(-90));
   mulPose(Axis.YP.rotationDegrees(180));
   mulPose(Axis.XP.rotationDegrees(90));   // 手臂前伸，需抬成垂直于手臂
   scale(16,16,16);
   translate(-1/16, 0.125, -0.625);        // 原版手部偏移，不能再乘缩放倍数
   ```
4. **重复缩放坑**：`scale` 之后的 `translate` 会被再放大；原版偏移不能再乘 16

## 七、动画范式（移植自 pi-blockbench）

### 核心原则

1. **时间量化到 20-tick 网格**：必须是 0.05 秒的倍数
2. **位置/旋转 = 加法轨道**（0 = 不偏移）；**缩放 = 乘法轨道**（1 = 中性）
3. **循环动画**：首尾姿势与**速度**都要连续；不能把所有轨道缓动到中性收尾。
   做法是让 `organicWave` 的 `cycles` 取整数，并让采样数整除 tick 总数
4. **主运动 + 滞后次运动 + 预备/跟随**，避免所有关节同步动
5. **有机波形**叠加谐波，不用纯正弦

### 有机波形实现

```java
// 主正弦 + 14% 二次谐波（2 倍频、相位偏移 0.7、转角 0.35）
static double organicWave(double t, double amplitude, int cycles, double phase) {
    double main   = Math.sin(Math.PI * 2 * cycles * t + phase);
    double detail = Math.sin(Math.PI * 4 * cycles * t + phase * 0.7 + 0.35);
    return amplitude * (main + 0.14 * detail);
}
```

### 动画清单

| 动画 | 时长 | 循环 | 说明 |
|---|---|---|---|
| idle | 3.0s | 是 | 呼吸起伏 + 脊椎波浪 + 缓慢环视 |
| walk | 1.6s | 是 | 大腿摆 ±28°，膝盖峰值滞后 0.4s，上身 Z 轴摇摆 ±12° |
| attack | 1.6s | 否 | 蓄力(0~0.6) → 挥砍(0.6~0.85) → 收势(0.85~1.6) |

## 八、Blockbench MCP 操作

- 插件位置：`%APPDATA%\Blockbench\plugins\`（**必须在 plugins 根目录**，
  放子文件夹不会被加载）
- MCP 服务：`http://localhost:3000/bb-mcp`
- 调用封装：`.tmp/mcp/bbmcp.ps1`（需 `-ExecutionPolicy Bypass`）
- 常用能力：`capture_screenshot`（**能让我看到模型**）、`list_outline`、
  `place_cube`、`modify_cube`、`set_cube_uv`、`risky_eval`、`export_model`

**`risky_eval` 禁用规则**：代码里不能含 `console.`、`//`、`/* */` 注释。

## 九、验证方式

```powershell
$env:JAVA_HOME='C:\dsh-tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME='E:\titanmod\.gradle-home'
$env:TMP='E:\titanmod\.tmp'; $env:TEMP='E:\titanmod\.tmp'
cd E:\titanmod\titanreborn
.\gradlew.bat build --no-daemon
.\gradlew.bat runClient --no-daemon
```

判断标准：`BUILD SUCCESSFUL` + 进游戏无 `ERROR`/`Exception` +
用户目视确认。**模型/动画的最终判断权在用户**（我看不到游戏画面）。
