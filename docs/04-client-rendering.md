# 04 · 客户端渲染与动画方案

> 本文件职责：确定巨型泰坦怎么渲染、怎么动、多部件怎么接。
> 本文件是 M1 垂直切片的技术核心——**泰坦看得见、动得起来、打得中**。

## 一、结论（先给方案）

| 决策点 | 选择 | 依据 |
|---|---|---|
| 动画方案 | **GeckoLib**（MIT，支持 1.21.1） | 手写 `ModelPart` 动画做不出原版那种流畅的巨兽动作；GeckoLib 有 Bedrock 动画管线，社区成熟 |
| 多部件命中 | **NeoForge 原生 `PartEntity` + `IEntityExtension`** | 已核验，见第三节；**不需要第三方** |
| 模型缩放 | 在渲染器里对 `ModelPart` 做缩放，**不换实体** | 换实体会丢状态、断 AI |
| 渲染器注册 | `EntityRenderersEvent.RegisterRenderers` | 已核验签名 |
| 模型层定义 | `EntityRenderersEvent.RegisterLayerDefinitions` | 已核验签名 |

> **风险提示**：引入 GeckoLib 意味着多一个依赖。若你希望**零第三方依赖**，退路是纯原版 `EntityModel`/`ModelPart` 手写动画——可行但工作量大幅上升，且巨兽的"重量感"很难做出来。**建议 M1 就用 GeckoLib 试一次**，用真实观感来定这件事。

## 二、GeckoLib 方案（主选）

### 2.1 已核实的事实

| 项 | 值 |
|---|---|
| Modrinth ID | `8BmcQJ2H` |
| 支持版本 | 含 **1.21.1** |
| 加载器 | Fabric / Forge / **NeoForge** / Quilt |
| 许可证 | **MIT** |
| 下载量 | 71,553,976 |

> 依据：`https://api.modrinth.com/v2/search?query=geckolib&facets=[["versions:1.21.1"],["categories:neoforge"]]`（2026-10-03 实测）

### 2.2 依赖怎么加（M0.4 已实测）

> ⚠️ **关键坑（已踩，务必避开）**：**不要用 Modrinth 的 GeckoLib jar**。
> Modrinth 上的是**运行时产物**，类引用的是中间映射名（`class_2960`、`class_897`…），
> 在开发环境编译会报一片「无法访问 `class_2960`」。
> 开发必须用 GeckoLib **官方 Maven**（按 Mojang 官方映射发布）。

```groovy
repositories {
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven'
        content { includeGroup 'software.bernie.geckolib' }
    }
}

dependencies {
    // 构件名带 MC 版本后缀；1.21.1 对应 geckolib-neoforge-1.21.1
    implementation 'software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3'
}
```

**实测结果（2026-10-03）**：

| 项 | 结果 |
|---|---|
| Cloudsmith 可达性 | ✅ 200（`maven-metadata.xml` 与 `4.9.3` 的 pom/jar 均 200） |
| 官方 jar 的映射 | ✅ 官方名（含 `ResourceLocation`，不含 `class_2960`） |
| Modrinth jar 的映射 | ❌ 中间名（含 `class_2960`）→ 开发环境不可用 |
| `gradlew build` | ✅ `BUILD SUCCESSFUL` |
| `runClient` | ✅ 日志出现 `GeckoLib 4 4.9.3 (geckolib)`、`mod/geckolib` 进入资源管理器，**无 geo/动画解析错误** |

> 运行时仍需玩家自行安装 GeckoLib 4.9.3（或用 Modrinth 那个 jar），
> 因为它是独立前置模组；上面的依赖只解决**开发期编译**。

### 2.3 资源路径规则（已实测确认）

GeckoLib 的 `DefaultedGeoModel` / `DefaultedEntityGeoModel` 按固定约定推导路径，
**子类只需给出一个 {@code ResourceLocation}**（我们用 `titanreborn:titan_zombie`）：

| 资源 | 推导出的路径 |
|---|---|
| 模型 | `assets/<ns>/geo/entity/<path>.geo.json` |
| 贴图 | `assets/<ns>/textures/entity/<path>.png` |
| 动画 | `assets/<ns>/animations/entity/<path>.animation.json` |

（依据：反编译 GeckoLib 4.9.3 的 `DefaultedGeoModel` 常量池，见
`geo/`、`.geo.json`、`animations/`、`.animation.json`、`textures/`、`.png`，以及
`DefaultedEntityGeoModel` 的 `entity` 片段。）

### 2.4 实际的类与 API（已用 javap 核验）

| 用途 | 类 / 方法 |
|---|---|
| 实体获得动画能力 | `implements GeoEntity`（接口，提供 `getAnimData`/`triggerAnim` 等默认方法） |
| 实例缓存 | `GeckoLibUtil.createInstanceCache(this)` → `AnimatableInstanceCache` |
| 注册控制器 | `registerControllers(AnimatableManager.ControllerRegistrar)` |
| 控制器 | `new AnimationController<>(this, "<名字>", <过渡tick>, state -> ...)` |
| 可触发动画 | `controller.triggerableAnim("<key>", RawAnimation)` + `triggerAnim(控制器名, key)` |
| 动画构建 | `RawAnimation.begin().thenLoop("<动画名>")` / `.thenPlay(...)` |
| 状态判定 | `AnimationState#isMoving()`、`#setAndContinue(RawAnimation)` |
| 模型基类 | `extends DefaultedEntityGeoModel<T>(ResourceLocation, boolean)` |
| 渲染器基类 | `extends GeoEntityRenderer<T>(EntityRendererProvider.Context, GeoModel<T>)` |


### 2.3 渲染器与实体类

GeckoLib 的接入模式（**实现时对照其官方文档核对类名**，此处只给结构）：

```
实体类            AbstractTitan extends PathfinderMob  →  实现 GeckoLib 的动画实体接口
模型类            TitanZombieModel        →  定义骨骼、贴图、动画文件
渲染器            TitanZombieRenderer     →  继承 GeckoLib 的渲染器基类
动画文件          assets/titanreborn/animations/entity/titan_zombie.animation.json
模型文件          assets/titanreborn/geo/entity/titan_zombie.geo.json
贴图              assets/titanreborn/textures/entity/titan_zombie.png
```

> **注意**：GeckoLib 用 **Bedrock 几何模型格式**（`.geo.json` + `.animation.json`），
> 与 Java 版的 `models/` JSON 是两套东西。这意味着泰坦模型**不能用 Blockbench 的 Java 模型导出**，
> 要用 Bedrock 模型格式导出。这一点会影响美术工作流，需提前告知做美术的人。

### 2.4 巨型实体的缩放

GeckoLib 模型在骨骼层面缩放即可（`RenderContext` 里对根骨骼 scale）。
**关键约束**：

1. 缩放只影响显示，**不影响碰撞箱** —— 碰撞箱在 `EntityType.Builder.sized(w, h)` 里单独设；
2. 两者必须**对齐**，否则会出现"看起来打中了却打不到"；
3. 建议：先定一个基准模型（1 倍），再按倍率同时放大碰撞箱与渲染缩放，倍率写进配置表。

## 三、多部件（可分别命中的部位）——已核验的官方机制

这是破防系统"打特定部位"的基础。**1.21.1 没有通用的 `EntityPart` 类，但 NeoForge 提供了官方机制。**

### 3.1 三个 API（全部已核验）

```java
// 1) 让实体声明自己是多部件实体（NeoForge 扩展接口，Entity 已实现该接口）
//    net.neoforged.neoforge.common.extensions.IEntityExtension
default boolean isMultipartEntity() { return false; }

//    返回值「应是缓存的」
@Nullable
default PartEntity<?>[] getParts() { return null; }

// 2) 部件基类
//    net.neoforged.neoforge.entity.PartEntity
public abstract class PartEntity<T extends Entity> extends Entity {
    private final T parent;
    public PartEntity(T parent) {
        super(parent.getType(), parent.level());
        this.parent = parent;
    }
    public T getParent() { return parent; }
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        throw new UnsupportedOperationException();   // 部件不需要独立同步包
    }
}

// 3) 世界侧查询：net.neoforged.neoforge.common.extensions.ILevelExtension
public default Collection<PartEntity<?>> getPartEntities()
```

### 3.2 官方语义（摘自 `IEntityExtension` 注释，逐字理解）

> "The entities returned by this method are NOT saved to the world in any way, they exist as an extension of their host entity.
> The child entity does not track its server-side (or client-side) counterpart, and the host entity is responsible for moving and managing these children."

含义：
- 部件**不被存档**、不独立存在；
- 部件**不自己同步**（`getAddEntityPacket` 直接抛异常）——**宿主实体负责移动与管理它们**；
- 因此宿主必须在每个 tick 里把部件的位置/朝向算出来并同步（客户端也要算，或用实体数据同步器同步必要参数）。

### 3.3 原版参考实现

`net.minecraft.world.entity.boss.enderdragon.EnderDragon` 是官方示例（注释里直接点名它）：
它持有 `EnderDragonPart[] subEntities` 与具名部件（head / neck / body / tail1~3 / wing1 / wing2），
每个部件在构造时给定尺寸，`EnderDragonPart` 的 `hurt` 把伤害转发给宿主：

```java
// EnderDragonPart 的关键实现（原版，可作写法参考）
@Override
public boolean hurt(DamageSource src, float amount) {
    return this.isInvulnerableTo(src) ? false : this.parentMob.hurt(this, src, amount);
}
```

### 3.4 我们的落地结构

```
AbstractTitan implements IEntityExtension（Entity 已实现，只需覆写）
├── private final TitanPart[] parts;          // 缓存，构造时建好
├── private final TitanPart head;
├── private final TitanPart chest;            // 破防要打的部位
├── private final TitanPart[] bonePlates;     // M1「僵尸泰坦」的 3 片腐骨甲片
│
├── @Override public boolean isMultipartEntity() { return true; }
├── @Override public PartEntity<?>[] getParts()  { return parts; }   // 返回缓存数组
└── @Override public void tick() { super.tick(); updatePartPositions(); }  // 宿主负责摆位

TitanPart extends PartEntity<AbstractTitan>
├── 具名字段 name
├── EntityDimensions size
├── hurt() 转发给宿主，并把「命中哪个部件」传给宿主的破防逻辑
└── isPickable() = true
```

**给破防系统的接口**：部件命中时把 `partName` 传进宿主，宿主的破防条件按 `partName` 计数。
这样"打碎 3 片腐骨甲片"就是 `bonePlates` 三个部件的命中计数，天然复用官方机制。

### 3.5 性能注意

- 部件数量**不要超过 8 个**（原版末影龙是 9 个，已是上限量级）；每个部件都要参与碰撞与追踪计算；
- `getParts()` 必须返回缓存数组（官方注释明确要求），**不要每次 new**；
- 宿主每 tick 更新部件位置时避免重复创建 `AABB` 对象（巨型实体部件 AABB 很大，GC 压力明显）。

## 四、渲染器注册（已核验签名）

```java
// net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers
public <T extends Entity> void registerEntityRenderer(
        EntityType<? extends T> entityType, EntityRendererProvider<T> provider)

// net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions
public void registerLayerDefinition(ModelLayerLocation loc, Supplier<LayerDefinition> supplier)
```

注册必须在**仅客户端**的事件总线处理器里做：

```java
@EventBusSubscriber(modid = TitanReborn.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TitanClientSetup {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TITAN_ZOMBIE.get(), TitanZombieRenderer::new);
    }
}
```

## 五、巨型实体的渲染追踪

巨型实体（8~20 倍体型）有两个必须注意的渲染参数，**都设在 `EntityType.Builder` 上**：

| 参数 | 建议值 | 不设的后果 |
|---|---|---|
| `clientTrackingRange(n)` | 16（区块） | 远处看不到泰坦，走到跟前才"突然出现" |
| `updateInterval(n)` | 2~3 | 每 tick 同步巨型实体的移动，网络流量浪费 |

## 六、待验证清单（M1 必须先做的实验）

| # | 实验 | 通过标准 | 阻塞什么 |
|---|---|---|---|
| 1 | GeckoLib 依赖能否装上（Maven 或本地 jar） | `gradlew build` 通过 | 整个渲染方案 |
| 2 | 一个自定义 `PartEntity` 能被攻击命中并转发伤害 | 日志打出部件名与宿主受伤 | 破防系统 |
| 3 | 部件位置随宿主移动正确更新 | 目视部件跟随、无抖动 | 破防系统 |
| 4 | GeckoLib 模型按倍率缩放后与碰撞箱对齐 | 目视命中位置正确 | 全部战斗 |
| 5 | `clientTrackingRange` 生效 | 远处可见 | 体验 |

**按 [06-roadmap.md](06-roadmap.md) 的 M1 定义，实验 1~4 是 M1 的"技术验证"前置条件。**

---

## 附：核验依据

- NeoForge 21.1.252 源码 jar：`E:\titanmod\.gradle-home\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.252\...\neoforge-21.1.252-sources.jar`
  - `net/neoforged/neoforge/common/extensions/IEntityExtension.java`（`isMultipartEntity` / `getParts`）
  - `net/neoforged/neoforge/common/extensions/ILevelExtension.java`（`getPartEntities`）
  - `net/neoforged/neoforge/entity/PartEntity.java`
  - `net/neoforged/neoforge/client/event/EntityRenderersEvent.java`
- Minecraft 1.21.1（NeoForge 补丁后）源码 zip：`E:\titanmod\.gradle-home\caches\neoformruntime\intermediate_results\sourcesWithNeoForge_*_output.zip`
  - `net/minecraft/world/entity/boss/EnderDragonPart.java`
  - `net/minecraft/world/entity/boss/enderdragon/EnderDragon.java`
- GeckoLib 元数据：Modrinth API `XGi9Z8am` 邻项查询（`8BmcQJ2H`），2026-10-03
