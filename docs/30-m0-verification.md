# M0 技术验证记录

> 本文件职责：记录 M0 各项实验的**实际结果**，作为 [06-roadmap.md](06-roadmap.md) 的进度证据。
> 工程：`E:\titanmod\titanreborn`（modid `titanreborn`，NeoForge 21.1.252 + MC 1.21.1）
> 日期：2026-10-03

## 一、结论汇总

| # | 实验 | 状态 | 证据 |
|---|---|---|---|
| 0.1 | 工程初始化 | ✅ 通过 | `gradlew clean build` → `BUILD SUCCESSFUL`，产物 `titanreborn-0.1.0.jar`（16056 字节） |
| 0.2 | 自定义实体 + 属性 + 渲染器 | ✅ 代码完成并通过构建；客户端启动到主菜单 | 见 §3、§4 |
| 0.3 | 多部件（`PartEntity`） | ✅ 编译通过；待游戏内交互确认（**人工**） | 见 §5 |
| 0.4 | GeckoLib 接入 | ⬜ **未做**（M0 剩余项） | 见 §7 |
| 0.5 | 伤害事件改写 | ✅ 编译通过；待游戏内交互确认（**人工**） | 见 §6 |

**M0 尚未收口**：0.4 未开始，0.2/0.3/0.5 的"游戏内行为"属人工验证项，尚未执行。

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

**待人工验证**：进世界用生成蛋刷出泰坦，打不同部位，确认日志里出现不同的部件名。

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

**待人工验证**：进世界打泰坦，未破防时应完全免伤；打碎 3 片甲片后同一攻击应造成实伤。

## 七、M0.4（GeckoLib）——未做

状态：**未开始**。方案见 [04-client-rendering.md](04-client-rendering.md) §2。

待办：
1. 确认 `api.modrinth.com/maven` 在本机网络下可达（REST 接口已实测可用，Maven 端点未测）；
   不可达则退回"下载 jar 放 `libs/`"。
2. 接一个最小 GeckoLib 实体，确认能渲染。
3. 确认 Bedrock 模型（`.geo.json`）缩放后与碰撞箱对齐。

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
