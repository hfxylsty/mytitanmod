# 02 · 工程与代码架构

> 本文件职责：确定工程怎么组织、代码分几层、每层用什么机制。**所有 API 均已对照 NeoForge 21.1.252 官方源码核验**（依据见文末）。

## 一、工程布局

```
titanreborn/                          # 模组工程根（规划，尚未创建）
├── build.gradle                      # ModDevGradle
├── settings.gradle                   # pluginManagement 仓库
├── gradle.properties                 # 版本号 + NeoForm 开关
├── gradle/wrapper/                   # wrapper 四件套
└── src/main/
    ├── java/com/titanreborn/
    │   ├── TitanReborn.java          # 主类：@Mod，只做总线挂载
    │   ├── registry/                 # 注册类，一注册表一文件
    │   ├── entity/                   # 实体：titan / minion / projectile
    │   ├── entity/ai/                # 自定义 AI 目标与行为
    │   ├── combat/                   # 伤害规则、破防、重生长
    │   ├── event/                    # 事件处理器
    │   ├── network/                  # 自定义载荷
    │   ├── config/                   # ModConfigSpec
    │   ├── command/                  # 指令
    │   ├── world/                    # 维度/生成（M3）
    │   └── client/                   # 仅客户端：renderer / model / layer
    └── resources/
        ├── META-INF/neoforge.mods.toml
        ├── pack.mcmeta
        ├── assets/titanreborn/       # 贴图 / 模型 / 语言 / 音效
        └── data/titanreborn/         # 配方 / 战利品 / 标签 / 维度 / 生物群系
```

**分层规则（硬约束）**：

1. `client/` 下的类**只能被客户端加载**，绝不从 `entity/`、`event/` 里直接引用——1.21.1 服务端没有客户端类，直接引用会在专用服务器上崩。
   需要客户端行为时，用 `DistExecutor`/`@EventBusSubscriber(value = Dist.CLIENT)` 或在客户端事件里注册。
2. 注册类只声明"有哪些东西"，不写逻辑；逻辑放对应模块。
3. 数据能放 JSON 的就不写进 Java（配方、战利品、标签、维度、生物群系）。

## 二、注册体系（1.21.1 正确写法）

### 2.1 总原则

- 每个注册表一个 `DeferredRegister`，集中在 `registry/` 包；
- 主类构造器接收事件总线，把注册器逐个 `register(bus)`；
- **注册类必须 `import` 到主类**，否则静态初始化不触发、内容不进注册表（最常见的"编译能过但游戏里没有"错误）。

主类形状（与已验证的模板一致）：

```java
package com.titanreborn;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TitanReborn.MODID)
public class TitanReborn {
    public static final String MODID = "titanreborn";

    public TitanReborn(IEventBus bus) {
        ModEntities.ENTITY_TYPES.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModSounds.SOUNDS.register(bus);
        // 新增注册器必须在这里登记，否则不生效
    }
}
```

### 2.2 各注册表的工厂方法（已核验）

| 内容 | 工厂方法 | 备注 |
|---|---|---|
| 物品 | `DeferredRegister.createItems(MODID)` | 返回 `DeferredRegister.Items` |
| 方块 | `DeferredRegister.createBlocks(MODID)` | 返回 `DeferredRegister.Blocks` |
| 创造物品栏分组 | `DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)` | **必写，否则物品栏里看不到物品**（见 2.4） |
| 其他（实体、音效、粒子…） | `DeferredRegister.create(Registry, MODID)` | 通用形式 |
| 实体类型 | `DeferredRegister.create(Registries.ENTITY_TYPE, MODID)` | 无专用便捷方法，用通用形式 |
| 数据组件 | `DeferredRegister.createDataComponents(MODID)` | 自定义组件用 |

### 2.4 物品注册了 ≠ 物品栏里有（M0 实测踩到的坑）

**注册进 `Registries.ITEM` 只是让物品"存在于注册表"，不会自动出现在创造模式物品栏。**
必须显式加入某个 `CreativeModeTab` 的展示列表，否则玩家的表现就是"这个物品不存在"。

```java
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TitanReborn.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TitanReborn.MODID + ".main"))
                    .icon(() -> new ItemStack(ModItems.FELLCORE_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        // 新增物品必须在这里登记
                        output.accept(ModItems.FELLCORE_SHARD.get());
                    })
                    .build());
}
```

语言文件里必须有 `itemGroup.titanreborn.main`，否则分组名显示为原始键名。

> 已核验：`Registries.CREATIVE_MODE_TAB`、`CreativeModeTab.builder()`、
> Builder 的 `title(Component)` / `icon(Supplier<ItemStack>)` / `displayItems(DisplayItemsGenerator)`。

便捷注册方法（已核验签名）：

```java
// 物品
DeferredItem<Item> registerSimpleItem(String name)
DeferredItem<Item> registerSimpleItem(String name, Item.Properties props)
// 方块
DeferredBlock<Block> registerSimpleBlock(String name)
DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties props)
// 方块的物品形式（含刷怪蛋以外的一般方块物品）
DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block)
DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block)
```

> 通用注册：`<I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup)`。
> 拿到的是 `DeferredHolder`，它实现 `Supplier`，取值用 `.get()`。

### 2.3 实体注册的完整链路（四步，缺一不可）

这是 1.21.1 最容易漏的地方——**实体不是注册一次就完事**：

```java
// 第 1 步：注册 EntityType
public static final DeferredHolder<EntityType<?>, EntityType<TitanZombie>> TITAN_ZOMBIE =
        ENTITY_TYPES.register("titan_zombie",
                () -> EntityType.Builder.of(TitanZombie::new, MobCategory.MONSTER)
                        .sized(4.0F, 12.0F)      // 碰撞箱：巨型实体必须放大
                        .clientTrackingRange(16) // 巨型实体的追踪范围要加大，否则远处不渲染
                        .build("titan_zombie"));

// 第 2 步：注册属性（MOD 总线事件）
@EventBusSubscriber(modid = TitanReborn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModAttributes {
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TITAN_ZOMBIE.get(),
                TitanZombie.createAttributes().build());
    }
}

// 第 3 步：客户端注册渲染器（仅客户端，MOD 总线，见 04-client-rendering.md）
// 第 4 步：刷怪蛋等物品（可选）
```

已核验的关键签名：

```java
// net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
public void put(EntityType<? extends LivingEntity> entity, AttributeSupplier map)
// 注意：重复注册同一个 EntityType 会抛 IllegalStateException

// net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers
public <T extends Entity> void registerEntityRenderer(
        EntityType<? extends T> entityType, EntityRendererProvider<T> provider)

// 模型层定义（RegisterLayerDefinitions）
public void registerLayerDefinition(ModelLayerLocation loc, Supplier<LayerDefinition> supplier)
```

## 三、实体框架分层

### 3.1 类层次设计

```
PathfinderMob (原版)
└── AbstractTitan                 # 所有泰坦的共同父类
    ├── TitanZombie               # M2
    ├── TitanSkeleton             # M2
    └── ...

AbstractTitanMinion               # 所有召唤物的共同父类
├── MinionLoyalist                # 忠诚者
├── MinionPriest                  # 牧师
├── MinionZealot                  # 狂热者
├── MinionBishop                  # 主教
└── MinionPaladin                 # 圣骑士
```

**为什么用两层而不是给每个泰坦写一个独立类**：五级召唤物的 AI 是**共用的**，只有数值和外观按泰坦变体化。用"通用模板 + 数据驱动变体"能把 M2 的 3~5 个泰坦 × 5 级召唤物从 25 个类压缩到 5 个类 + 一张配置表。

### 3.2 巨型实体的三个技术难点

| 难点 | 1.21.1 的做法 | 风险 |
|---|---|---|
| **碰撞箱** | `EntityType.Builder.sized(w, h)` 直接给大尺寸 | 低。但要注意：碰撞箱过大会导致寻路失败，需要配 `PathNavigation` 或放宽 |
| **多部件（可分别命中/破坏的部位）** | **NeoForge 原生支持**：覆写 `IEntityExtension.isMultipartEntity()` 返回 true、`getParts()` 返回缓存的 `PartEntity<?>[]`；部件类继承 `net.neoforged.neoforge.entity.PartEntity<T>`。原版末影龙即示例实现 | ✅ **已解决**，详见 [04-client-rendering.md](04-client-rendering.md) 第三节 |
| **追踪范围** | `clientTrackingRange(n)`，巨型实体建议 16 以上 | 低 |

> **已核验的多部件 API**（NeoForge 21.1.252 源码）：
> ```java
> // net.neoforged.neoforge.common.extensions.IEntityExtension
> default boolean isMultipartEntity() { return false; }
> @Nullable default PartEntity<?>[] getParts() { return null; }   // 注释要求返回值应缓存
>
> // net.neoforged.neoforge.entity.PartEntity
> public abstract class PartEntity<T extends Entity> extends Entity {
>     public PartEntity(T parent);
>     public T getParent();
>     // getAddEntityPacket 抛 UnsupportedOperationException —— 部件不独立同步，由宿主管理
> }
> ```
> 官方语义：部件**不存档、不独立同步**，宿主实体负责每 tick 摆放它们。
> 这让"打碎 3 片腐骨甲片"这类破防条件可以直接用官方机制实现。

### 3.3 属性与体型

巨型实体的"体型倍率"**不要通过修改模型缩放实现**，而是：

1. 实体本身的碰撞箱与 `Attributes.SCALE`（若需要）按倍率设置；
2. 渲染时对 `ModelPart` 做缩放（见 [04-client-rendering.md](04-client-rendering.md)）；
3. 攻击范围用 `Attributes.ENTITY_INTERACTION_RANGE` 或自定义判定距离。

## 四、数据驱动层

1.21.1 下这些**必须是 JSON**，不能像 1.12.2 那样写在 Java 里：

| 内容 | 目录 | 备注 |
|---|---|---|
| 合成/熔炼配方 | `data/titanreborn/recipe/` | 单数 `recipe`（1.21 起） |
| 战利品表 | `data/titanreborn/loot_table/` | 单数 `loot_table` |
| 物品/方块标签 | `data/titanreborn/tags/item/`、`tags/block/` | 单数 |
| 维度 | `data/titanreborn/dimension/` | M3 |
| 生物群系 | `data/titanreborn/worldgen/biome/` | M3 |
| 伤害类型 | `data/titanreborn/damage_type/` | 窃魂/虚空伤害需要 |

> 目录名用**单数**是 1.21 的关键变化，写成复数（`recipes/`）不会报错但**不生效**——这是最常见的静默失败。

## 五、网络

多人同步在 1.21.1 用 **自定义载荷（CustomPacketPayload）+ `PayloadRegistrar`**，不再有 `SimpleNetworkWrapper`。
具体写法与最小示例见 [03-gameplay-systems.md](03-gameplay-systems.md) 的"同步"节，API 细节在实现时对照 NeoForge 源码核验。

## 六、配置

用 NeoForge 的 `ModConfigSpec`（`@Config`/`ConfigManager` 是 1.12.2 的写法，已不适用）。
至少要暴露：双模式开关、泰坦生成率、召唤物上限、伤害规则开关（便于调试与平衡）。

## 七、性能预算（设计值，待压测校准）

巨型实体 + 大量召唤物是这个模组最大的性能风险。硬指标：

| 项 | 目标 | 措施 |
|---|---|---|
| 同屏泰坦数 | ≤4 | 生成规则限流 |
| 单泰坦召唤物上限 | 忠诚者 120 / 牧师 60 / 狂热者 30 / 主教 15 / 圣骑士 6（**参考原版设计值，需重平衡**） | 全局计数 + 超限不生成 |
| 召唤物 AI 开销 | 距离玩家 >64 格时降频 | 自定义 `Goal` 里做 tick 分频 |
| 区块加载 | 不主动强加载区块 | 原模组的 `MobChunkLoader` 思路**不采用**，改用原版追踪范围 |

## 八、核验依据

本文件的 API 结论来自 NeoForge 21.1.252 官方源码 jar：

```
E:\titanmod\.gradle-home\caches\modules-2\files-2.1\net.neoforged\neoforge\
  └── 21.1.252\...\neoforge-21.1.252-sources.jar
```

已逐字核验的文件：
- `net/neoforged/neoforge/registries/DeferredRegister.java` —— 工厂方法、便捷注册方法签名
- `net/neoforged/neoforge/event/entity/EntityAttributeCreationEvent.java` —— `put` 签名与重复注册异常
- `net/neoforged/neoforge/client/event/EntityRenderersEvent.java` —— `RegisterRenderers` / `RegisterLayerDefinitions` 内部类与方法签名

核验方法（可复现）：

```powershell
$jar = Get-ChildItem 'E:\titanmod\.gradle-home\caches\modules-2\files-2.1\net.neoforged\neoforge' -Recurse -Filter '*sources.jar' | Select-Object -Last 1
# 用解压工具打开该 jar，读上面列出的 .java 文件
```

**未核验项**（实现时必须先查再写）：`EntityPart` 在 1.21.1 的可用性、`PayloadRegistrar` 的具体用法、数据驱动维度的完整字段。
