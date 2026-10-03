# M0 技术验证记录

> 本文件职责：记录 M0 各项实验的**实际结果**，作为 [06-roadmap.md](06-roadmap.md) 的进度证据。
> 工程：`E:\titanmod\titanreborn`（modid `titanreborn`，NeoForge 21.1.252 + MC 1.21.1）
> 日期：2026-10-03

## 一、结论汇总

| # | 实验 | 状态 | 证据 |
|---|---|---|---|
| 0.1 | 工程初始化 | ✅ 通过 | `gradlew clean build` → `BUILD SUCCESSFUL` |
| 0.2 | 自定义实体 + 属性 + 渲染器 | ✅ 通过（含人工） | 见 §3、§4；用户已确认能在游戏内刷出泰坦 |
| 0.3 | 多部件（`PartEntity`） | ✅ 通过（含人工） | 见 §5；但**部件命中日志当时被伤害规则拦在前面**，待 M1 复验 |
| 0.4 | GeckoLib 接入 | ✅ **通过** | 见 §7：官方 Maven 依赖 + geo/动画文件 + 渲染器全部落地，客户端无解析错误 |
| 0.5 | 伤害事件改写 | ✅ 通过（含人工） | 见 §6；实测日志见下 |

**M0 五项全部有结论。** 实测证据（2026-10-03 人工验证，用户操作）：

```
[Server thread/INFO] [titanreborn/combat/]: [M0.5] 泰坦受击 原始=8.0 最终=0.0 判定=未破防 破防=false 来源=player
[Server thread/INFO] [titanreborn/combat/]: [M0.5] 泰坦受击 原始=12.0 最终=0.0 判定=未破防 破防=false 来源=player
```

连续多次攻击伤害全部归零 → **未破防免疫生效**。

> **一处待复验**：日志里只出现了 `[M0.5]`（伤害规则），没出现 `[M0.3]`（部件命中）。
> 原因是 `TitanPart.hurt()` 转发到 `hurtPart()` 打日志后，最终仍走 `LivingIncomingDamageEvent`，
> 而后者把伤害归零并打了 `[M0.5]`。两条日志的先后与可见性需要 M1 复验一次，
> 确认「打中不同部位能区分出部件名」。

## 二、按 skill 口径的验收结果

技能 `minecraft-modding` 对现代线（NeoForge 1.21.x）规定的收尾验证是：
**`gradlew build` 通过 + `runClient` 启动到主菜单且日志无致命错误 + 人工功能操作标注"人工"**。

| 验收项 | 结果 | 日志证据 |
|---|---|---|
| 构建通过 | ✅ | `BUILD SUCCESSFUL in 13s` |
| 客户端到主菜单 | ✅ | `[Render thread/INFO] [minecraft/Minecraft]: Setting user: Dev` |
| 渲染后端就绪 | ✅ | `[Render thread/INFO] [minecraft/Minecraft]: Backend library: LWJGL version 3.3.3+5` |
| 模组被加载 | ✅ | `Titan Reborn 0.1.0 (titanreborn)` |
| 资源包含本模组 | ✅ | `Reloading ResourceManager: vanilla, mod_resources, mod/titanreborn, mod/neoforge` |
| NeoForge 版本 | ✅ | `NeoForge mod loading, version 21.1.252, for MC 1.21.1` |
| 日志无致命错误 | ✅ | 无 `FATAL` / `CrashReport` / `Exception in thread` |

## 三、M0.2 实体注册（四步链路全部落地）

代码位置（对应 [02-architecture.md](02-architecture.md) §2.3 的四步）：

| 步骤 | 文件 | 要点 |
|---|---|---|
| 1. `EntityType` | `ModEntities.java` | `DeferredRegister.create(Registries.ENTITY_TYPE, MODID)`；`sized(4.0F, 12.0F)`、`clientTrackingRange(16)`、`updateInterval(2)` |
| 2. 属性 | `ModAttributes.java` | `EntityAttributeCreationEvent.put(...)`，MOD 总线 |
| 3. 渲染器 | `client/TitanClientSetup.java` | `EntityRenderersEvent.RegisterRenderers`，**限定 `Dist.CLIENT`** |
| 4. 生成蛋 | `ModItems.java` | `DeferredSpawnEggItem`（接受 `Supplier<EntityType>`，避免时序冲突） |

### 关键决策：模型类的选择

原计划直接用原版 `ZombieModel`，但它的类型参数是 **`ZombieModel<T extends Zombie>`**——要求实体是 `Zombie` 的子类。
我们的泰坦统一继承 `Monster`（否则每只泰坦都得继承不同的原版怪，无法共用基类），**类型不匹配**。

解决：自建 `TitanZombieModel extends HumanoidModel<TitanZombie>`。`HumanoidModel<T extends LivingEntity>` 无额外约束，
骨架直接复用原版僵尸模型的层定义 `ModelLayers.ZOMBIE`。

### 缩放与碰撞箱必须对齐

- 碰撞箱：`EntityType.Builder.sized(4.0F, 12.0F)`（约 6 倍僵尸）
- 渲染缩放：`TitanZombieRenderer.scale()` 里 `poseStack.scale(6.0F, 6.0F, 6.0F)`

两者不一致会导致"看起来打中了却打不到"。**此对齐关系是人工验证项。**

## 四、编译期踩到的坑（已解决）

| 坑 | 现象（报错原文） | 修法 |
|---|---|---|
| 缺少抽象方法实现 | `TitanPart 不是抽象的, 并且未覆盖 Entity 中的抽象方法 defineSynchedData(Builder)` | 补 `defineSynchedData(SynchedEntityData.Builder)` 空实现（原版 `EnderDragonPart` 同样为空实现） |
| ModDevGradle 没有 `setForceExit` | `Could not find method setForceExit() for arguments [false] on Run[gameTestServer]` | 该 API 属于 NeoGradle，不适用于 ModDevGradle 2.0.143；删除该行 |

### 附带发现：物品注册了但物品栏里没有

第一次 `runClient` 后用户反馈"没有僵尸泰坦生成蛋"。原因不是注册失败，而是
**物品注册进 `Registries.ITEM` 并不会自动出现在创造模式物品栏里**——
必须显式加入某个 `CreativeModeTab` 的展示列表。

修法：新增 `ModCreativeTabs`，用 `DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)`
注册本模组分组 `titanreborn:main`，在 `displayItems(...)` 里逐个 `output.accept(...)`。

> **这是新手的经典误区，已回写本文件与 `02-architecture.md` 的注册体系一节。**

## 五、M0.3 多部件实现（依据 NeoForge 官方机制）

### 核验到的 API

```java
// net.neoforged.neoforge.common.extensions.IEntityExtension（Entity 已实现该接口）
default boolean isMultipartEntity() { return false; }
@Nullable default PartEntity<?>[] getParts() { return null; }   // 注释要求返回值「应缓存」

// net.neoforged.neoforge.entity.PartEntity
public abstract class PartEntity<T extends Entity> extends Entity {
    public PartEntity(T parent);
    public T getParent();
    // getAddEntityPacket 抛 UnsupportedOperationException —— 部件不独立同步
}
```

官方语义（逐字）：部件**不存档、不独立同步**，**宿主负责每帧摆放它们**。

### 我们的实现

- `entity/part/TitanPart.java`：`extends PartEntity<TitanZombie>`，含 `partName`、尺寸、相对偏移、`hitCount`
- `TitanZombie`：构造时建好 5 个部件并缓存成数组（head / chest / bone_plate_0~2）；
  `tick()` 里按宿主朝向旋转偏移后摆放每个部件；`remove()` 里同步移除部件
- 部件 `hurt()` 转发到 `TitanZombie.hurtPart()`，日志打出**命中哪个部件**——这是破防系统的数据来源

**人工验证状态**：用户已确认能在游戏内生成泰坦并攻击。
但日志里只见 `[M0.5]`（伤害规则），**未见 `[M0.3]`（部件命中）**——
因为部件受击后转发的伤害最终被伤害规则归零，两条日志的可见性需要在 M1 复验一次，
确认「打中不同部位能区分出部件名」。这是 M1 破防系统开工前必须确认的一件事。

## 六、M0.5 伤害管线

`combat/TitanDamageRules.java` 挂在 `LivingIncomingDamageEvent`（游戏总线，可取消）。
已核验可用方法：`getAmount` / `setAmount` / `addReductionModifier` / `setInvulnerabilityTicks`。

实现顺序（对应 [03-gameplay-systems.md](03-gameplay-systems.md) §1.1）：

```
目标是泰坦？
 ├─ 未破防        → 伤害归零
 ├─ 低于门槛(20)  → 归零
 ├─ 超过上限(1000)→ 截断
 └─ 正常          → 放行
最后统一 setInvulnerabilityTicks(30)  // 1.5 秒伤害冷却
```

破防判定（M0 临时版）：3 片腐骨甲片都被击中过即破防。

**人工验证状态**：已实测「未破防完全免伤」成立（见 §1 日志）。
「打碎 3 片甲片后造成实伤」待 M1 复验。

## 七、M0.4 GeckoLib 接入 —— ✅ 已完成

### 7.1 关键坑：Modrinth 的 jar 在开发环境不可用

第一次接依赖用了 Modrinth Maven：

```groovy
implementation 'maven.modrinth:geckolib:4.9.3'   // ❌ 编译期报一片「无法访问 class_2960」
```

报错形态：

```
错误: 无法访问class_2960
错误: 无法访问class_897
错误: 不兼容的类型: class_2960无法转换为ResourceLocation
```

**原因**：Modrinth 上的是**运行时产物**，类引用的是中间映射名（`class_2960` 等），
靠 ModLauncher 在运行时重映射；开发环境直接编译就会撞上。

**正解**：用 GeckoLib 官方 Maven（按 Mojang 官方映射发布）：

```groovy
repositories {
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven'
        content { includeGroup 'software.bernie.geckolib' }
    }
}
dependencies {
    implementation 'software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3'
}
```

两个 jar 的对照（实测）：

| 来源 | 是否含 `class_2960`（中间名） | 是否含 `ResourceLocation`（官方名） | 开发可用 |
|---|---|---|---|
| Modrinth | 是 | — | ❌ |
| Cloudsmith 官方 | 否 | 是 | ✅ |

### 7.2 落地的资产与代码

| 文件 | 作用 |
|---|---|
| `geo/entity/titan_zombie.geo.json` | Bedrock 几何模型，9 个骨骼（body/head/双腕/双腿 + 3 片甲片） |
| `animations/entity/titan_zombie.animation.json` | 三个动画：idle / walk / attack |
| `textures/entity/titan_zombie.png` | 128×128 占位贴图（按骨骼分区上色 + 噪点） |
| `client/model/TitanZombieModel.java` | `extends DefaultedEntityGeoModel<TitanZombie>` |
| `client/renderer/TitanZombieRenderer.java` | `extends GeoEntityRenderer<TitanZombie>` |
| `entity/TitanZombie.java` | 实现 `GeoEntity`；注册控制器；`triggerAnim` 播攻击 |

> 模型与动画文件由 `MakeTitanAssets` 生成器产出（不是手写 JSON），
> 改尺寸只需改生成器里的常量。生成器在 `.tmp/titanassets/`（临时产物，不入库）。

### 7.2.1 ⚠️ 第一版模型是错的（已修，教训记录）

第一版生成器产出的 geo 文件有**三处硬错误**，导致游戏内模型完全不正确：

| 错误 | 后果 |
|---|---|
| `cubes.origin` 用了「相对 pivot 的偏移」，而 Bedrock 规范要求**模型空间绝对坐标** | 所有部件叠在一起并沉到地面以下 |
| 所有 cube 的 `uv` 都是 `[0,0]` | 9 个部件共用贴图同一区域，外观全一样 |
| 骨骼没有 `parent`，全是平级 | 挥臂动画不会带动手臂（没有可动层级） |

修正后（已 `ConvertFrom-Json` 校验）：

| 骨骼 | parent | Y 范围（像素） | uv |
|---|---|---|---|
| `body` | 根 | 65 ~ 130 | 0,44 |
| `head` | body | 136 ~ 176 | 0,0 |
| `bone_plate_0/1/2` | body | 72 ~ 92 | 44/60/76, 44 |
| `right_arm` / `left_arm` | body | 60 ~ 130 | 0,112 |
| `right_leg` / `left_leg` | 根 | 0 ~ 65 | 92,112 |

总高 176 像素 ≈ 11 格，与碰撞箱 `4.0 × 12.0` 基本对齐。

> **更深一层的教训**：这个模型是"看不见结果的情况下手搓"的，所以第一版错得很彻底。
> **正规做法是用 Blockbench 做模型与动画**（可视化编辑 + 导出 Bedrock 格式），
> 代码侧只负责实体类/渲染器/模型类三件事。
> 当前生成的模型只是**占位**，正式模型规格见
> [31-art-spec-titan-zombie.md](31-art-spec-titan-zombie.md)。

### 7.3 资源路径规则（实测确认）

`DefaultedEntityGeoModel` 按约定推导，子类只给一个 `ResourceLocation`（我们用 `titanreborn:titan_zombie`）：

| 资源 | 路径 |
|---|---|
| 模型 | `assets/<ns>/geo/entity/<path>.geo.json` |
| 贴图 | `assets/<ns>/textures/entity/<path>.png` |
| 动画 | `assets/<ns>/animations/entity/<path>.animation.json` |

### 7.4 验收结果

| 项 | 结果 |
|---|---|
| `gradlew build` | ✅ `BUILD SUCCESSFUL` |
| 客户端加载 GeckoLib | ✅ 日志：`GeckoLib 4 4.9.3 (geckolib)`、`mod/geckolib` 进资源管理器 |
| geo / 动画解析 | ✅ **无任何解析错误**（说明生成的文件格式有效） |
| 客户端到主菜单 | ✅ `Setting user: Dev` |

**待人工验证**：进游戏刷出泰坦，确认新模型可见、待机/行走动画在播、体型与碰撞箱对齐。

> 顺带说明：日志里的 `Reference map 'geckolib.refmap.json' ... could not be read`
> 是**开发环境的正常提示**（Mixin 引用映射文件只在生产构建中生成），可忽略；
> `Sound engine/ERROR Stop: Invalid name parameter` 是关窗时的 OpenAL 清理噪音，与本模组无关。

## 八、GameTest 的尝试与结论（避免重复踩坑）

**我们尝试过用 GameTest 把验收自动化，最终放弃。** 结论与原因记录如下，供后续决策参考：

- `gameTestServer` 这个 run 类型**不是 ModDevGradle 2.0.143 的 `runs {}` 内置类型**，
  可以声明成功（`type = 'gameTestServer'`），任务也会生成；
- NeoForge 读的是**系统属性** `neoforge.enabledGameTestNamespaces`（不是 Gradle 属性），
  声明后日志确实变为 `Enabled Gametest Namespaces: [titanreborn]`；
- **卡点**：`@GameTest` 需要一个已存在的结构模板。原版**不提供** `minecraft:empty`，
  默认模板解析必然失败（`Missing test structure`）。我们手工生成了
  `data/titanreborn/structure/gametest_empty.nbt`（gzip NBT，110 字节），
  路径与官方文档一致，但 `StructureTemplateManager` 仍报找不到该结构，原因未查明。
- **决策**：**停止在这上面投入**。理由：① GameTest 不属于 `minecraft-modding` 技能承诺的能力范围，
  技能对现代线的验收口径是 "build + runClient 到主菜单 + 人工功能操作"；
  ② 结构模板问题与本机环境纠缠，继续排查的性价比低；
  ③ M0 的目的（验证技术可行性）已由编译通过与客户端启动达成。
- 已移除相关代码与配置，工程回到 skill 的标准路径。
- **如果将来再启动自动化测试**：建议从"用结构方块在游戏内导出一个真实结构文件"入手，
  而不是手工构造 NBT。

## 九、环境侧的新发现（已补进 20-environment.md）

**显卡首选项与 JDK 路径绑定**：`runClient` 在本工程首次运行即崩于 `atio6axx.dll`
（`EXCEPTION_ACCESS_VIOLATION`，与 `example-mod` 当初同一问题）。
原因是本次改用工作区外 JDK（`C:\dsh-tools\...`），而显卡首选项当初只对 `E:\titanmod\.tools\...` 设过。
补齐 `C:\dsh-tools\...\bin\java.exe` 的 `GpuPreference=2` 后，客户端正常启动。

> **教训**：换 JDK 路径后必须重设显卡首选项，否则 `runClient` 必崩。

## 十、下一步

1. 执行 §5/§6 的人工验证（进世界刷泰坦、打部位、看破防）——**需要人工操作**；
2. 完成 M0.4（GeckoLib 接入）；
3. 修掉生成蛋模型缺失警告（已补 `models/item/titan_zombie_spawn_egg.json`，待下次启动确认）；
4. M0 五项全部有结论后，再进 M1 垂直切片。
