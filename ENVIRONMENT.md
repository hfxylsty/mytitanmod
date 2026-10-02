# titanmod 开发环境说明（NeoForge 1.21.1）

> 记录日期：2026-10-03（最后更新：2026-10-03）
> 环境已装好并可用，`gradlew build` 与 `gradlew runClient` 均已实测通过。
> **权威版本以 `example-mod/build.gradle` 为准**（代码真相优先），本文件与之保持一致。

## 一、已装内容

| 组件 | 版本 | 位置 |
|---|---|---|
| JDK（主用） | Microsoft OpenJDK **21.0.12.1 LTS** | `E:\titanmod\.tools\jdk-21.0.12.1+1` |
| JDK（备用，走原生 javac） | 同上 | `C:\dsh-tools\jdk-21.0.12.1+1` |
| Gradle | **8.14**（wrapper 自动下载，腾讯云镜像） | `E:\titanmod\.gradle-home\wrapper\dists\` |
| NeoForge | **21.1.252**（对应 MC 1.21.1） | Gradle 缓存 |
| ModDevGradle | **2.0.143** | Gradle 缓存 |
| NeoForm Runtime | 2.0.24 | Gradle 缓存 |
| mod 工程 | `titanmod` / `com.titanmod` | `E:\titanmod\example-mod` |
| 技能 | `minecraft-modding`（v0.1.0） | `E:\titanmod\.dsh\skills\minecraft-modding` |

> **版本说明**：技能文档 `70-version-matrix.md` 与 `00-scaffold.md` 记录的是 **21.1.248**（该技能的实测版本）。
> 本地工程在排查过程中升到了 **21.1.252**（21.1 分支共 249 个版本，249~252 均在 `maven.neoforged.net` 上存在，已实测 200）。
> 两者都可运行；**以本工程 `build.gradle` 里的值为准**。若要回到技能口径，把 `build.gradle` 改回 `21.1.248` 即可。
> 21.1 分支的版本号规则：`<MC次版本>.<MC补丁>.<NeoForge构建号>`，即 `21.1.x` ↔ MC 1.21.1。

## 二、每次开新终端要设的环境变量

```powershell
$env:JAVA_HOME       = 'E:\titanmod\.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME = 'E:\titanmod\.gradle-home'
$env:PATH             = "$env:JAVA_HOME\bin;$env:PATH"
$env:TMP              = 'E:\titanmod\.tmp'
$env:TEMP             = 'E:\titanmod\.tmp'
```

`JAVA_HOME` 必须指向 JDK 21：NeoForge 1.21.x 用 JDK 8/17 会报 toolchain 或 class 版本错误。
本机系统 `java` 仍是 JDK 1.8，**不要**依赖 PATH 里那个 `java`。

## 三、常用命令

```powershell
cd E:\titanmod\example-mod
.\gradlew.bat build        # 编译，产物在 build\libs\
.\gradlew.bat runClient    # 启动游戏到主菜单（冒烟验证）
.\gradlew.bat runServer    # 启动服务端
```

首次构建会下载 Gradle 8.14 + Minecraft 资源并反编译（约 8 分钟），之后走缓存。

## 四、工程结构

```
example-mod/
├── build.gradle              # ModDevGradle 2.0.143 + NeoForge 21.1.248
├── settings.gradle           # pluginManagement 仓库（缺了会报插件找不到）
├── gradle.properties         # jvmargs / daemon / java.io.tmpdir
├── gradle/wrapper/           # wrapper 四件套，指向腾讯云 Gradle 8.14 镜像
└── src/main/
    ├── java/com/titanmod/
    │   ├── TitanMod.java     # @Mod(TitanMod.MODID) 主类，注册入口
    │   ├── ModBlocks.java    # DeferredRegister.Blocks
    │   └── ModItems.java     # DeferredRegister.Items
    └── resources/
        ├── META-INF/neoforge.mods.toml   # modId="titanmod"
        ├── pack.mcmeta                   # pack_format 34（1.21.1）
        ├── assets/titanmod/              # 贴图 / 模型 / blockstate / 语言
        └── data/titanmod/                # 配方 / 标签 / 战利品
```

命名空间已从模板的 `examplemod` 全部改为 `titanmod`（Java 包名、`@Mod`、`neoforge.mods.toml`、
`assets/`、`data/`、模型与配方 JSON 内的引用）。

## 五、网络限制（重要）

本机 `github.com` / `api.github.com` 的 DNS 被解析到 `127.0.0.1`（劫持），因此：

- ⚠️ `git clone https://github.com/...`、打开 github.com 网页 —— **取决于加速器是否在运行**
  （详见下方"实测更新"）
- ✓ `raw.githubusercontent.com`（取单个文件）
- ✓ `codeload.github.com`（取整个仓库 zip，**实测可用**）
- ✓ `data.jsdelivr.com`（列仓库文件树）
- ✓ `ghproxy.net`、`ghfast.top`、`gh-proxy.com`（GitHub 代理）
- ✓ `maven.neoforged.net`、`repo.maven.apache.org`、`plugins.gradle.org`
- ✓ `piston-meta.mojang.com`、`piston-data.mojang.com`、`libraries.minecraft.net`
- ✓ `mirrors.cloud.tencent.com`（Gradle 8.14 镜像）
- ✓ `api.modrinth.com`（模组元数据查询）
- ✓ `www.mcmod.cn`（中文模组资料）
- ✗ `www.curseforge.com`（403，Cloudflare 拦截）
- ✗ `minecraft.wiki` 网页版（403）；改走 `minecraft.wiki/api.php` 可用

### 实测更新（2026-10-03，重要更正）

早期记录写"git clone 全部失败"，**该结论现在不成立**：

```
$ git ls-remote --heads https://github.com/Mrbt0907/The-Titans-Mod
a4d3fe60b068a621af3438bb74d4090484a0d758        refs/heads/1.12.2
cbb5b98ea53fc2948e1f1ef4dc775e2b2dfe7392        refs/heads/1.12.2-0.7
77cd6321ccbcd7b75c2cf2b53d29925ec5803b1f        refs/heads/1.7.10-0.5
a4d3fe60b068a621af3438bb74d4090484a0d758        refs/heads/main
exit=0
```

原因：本机 `0.0.0.0:443` 由 `Steam++.Accelerator` 进程监听，它会接管 GitHub 流量。
**但这依赖该加速器处于运行状态**，DNS 劫持本身仍在，所以可能时好时坏。

**结论：仍以 `codeload` / `raw.githubusercontent.com` 作为默认下载手段，git 作为备选。**
遇到 git 突然可用时不必惊讶；突然不可用也不要当成环境坏了。

### 取 GitHub 文件的推荐命令

```powershell
# 整个仓库（最稳）
curl.exe -sL -o repo.zip "https://codeload.github.com/<owner>/<repo>/zip/refs/heads/<branch>"
# 单个文件
curl.exe -sL "https://raw.githubusercontent.com/<owner>/<repo>/<branch>/<path>"
# 列文件树（无需 token）
curl.exe -sL "https://data.jsdelivr.com/v1/packages/gh/<owner>/<repo>"
```

## 六、已解决的问题：NeoForm 重编译失败（重要，换机器/换版本时会再遇到）

### 现象

`gradlew build` 在 `:createMinecraftArtifacts` 任务失败：`Node action for recompile failed`
→ `java.io.IOException: Compilation failed`，`recompile/output.jar` 是 22 字节的空 jar。
NeoForm 默认几乎不输出 javac 诊断，只能看到一行：

```
 ERROR Line: 1, ..net.minecraft.world.ticks.package-info...: null in /net/minecraft/world/ticks/package-info.java
```

`null` 这个错误消息具有误导性——看起来像源码问题，其实不是。

### 排查过程（结论都有证据）

1. **打开 NeoForm 详细日志**：Gradle 属性 `neoForge.neoFormRuntime.verbose=true`
   （对应 `NeoFormRuntimeExtension.getVerbose()`，即 `neoForge.neoFormRuntime.verbose`）。
   打开后真实错误现形：

   ```
   Compiling 5364 source files
   ERROR Line: 1, error while writing net.minecraft.world.ticks.package-info: null in /net/minecraft/world/ticks/package-info.java
   ```

   → 是 **`error while writing`（写 class 文件失败）**，不是编译语义错误。

2. **排除源码与 classpath**：用与 NeoForm 完全相同的 **110 项 classpath**
   （74 个 Minecraft 库 + 48 个 NeoForge 库/模块，由 `neoforge-*-moddev-config.json` 推导，
   数量与日志里「110 items on the compile classpath」完全吻合）
   手工编译全部 **6317** 个源文件 → **javac exit=0，产出 9768 个 class，零错误**。
   源码和 classpath 都是好的。

3. **定位到写入层**：NeoForm 的 `RecompileSourcesActionWithJDK` 用
   `FileSystems.newFileSystem(URI.create("jar:"+output.toUri()), Map.of("create", TRUE))`
   打开输出 zip，再用进程内 `javax.tools.JavaCompiler` 写入。
   复现该写入方式（见 `.tmp/zipwrite/ZipWriteTest.java`）报：

   ```
   java.nio.file.ReadOnlyFileSystemException
       at jdk.zipfs/jdk.zipfs.ZipFileSystem.checkWritable(ZipFileSystem.java:370)
       at jdk.zipfs/jdk.zipfs.ZipFileSystem.createDirectory(ZipFileSystem.java:708)
   ```

   → 是 **JDK zipfs 层拒绝写入**，累积到第 5364 个文件的父目录创建时才炸。

4. 已排除：NeoForge 补丁版本（21.1.248 与 21.1.252 同样报错）、
   缺件（48 个 NeoForge 库全在缓存里）、磁盘空间（138GB 可用）、
   工作区权限（已修）、系统临时目录（可写，且已重定向到 `E:\titanmod\.tmp`）。

### 修复

在 `example-mod/gradle.properties` 里改用 NeoForm 自带的 **ECJ（Eclipse 编译器）**，
它不走 JDK zipfs 的写入路径：

```properties
neoForge.neoFormRuntime.verbose=true
neoForge.neoFormRuntime.useEclipseCompiler=true
```

改完 `gradlew build` → **BUILD SUCCESSFUL**，产物 `build/libs/titanmod-1.0.0.jar`，
内含主类、`assets/titanmod/`、`data/titanmod/`、`neoforge.mods.toml`、`pack.mcmeta`。

> 副作用：ECJ 与 javac 在极端情况下对个别写法宽容度不同。若将来某个写法 ECJ 报错而 javac 不报，
> 记得这层差异；需要切回 javac 时删掉 `useEclipseCompiler` 那行即可（但会复现上面的 zipfs 问题）。

### 更深一层的原因（已由独立排查 + 实测确认）

JDK 的 zipfs 在构造时用 `readOnly = !Files.isWritable(zipPath)` 判断可写性。
**DSH 会把工作区（含 `E:\titanmod`）内的可执行文件以 Low 完整性级别启动**，
Low IL 进程里 `Files.isWritable()` 对本机任何路径都返回 `false`——即使真实写入是成功的。
于是 zipfs 把 `output.jar` 判成只读 → javac 一个 class 都写不进去 → `Compilation failed`。

决定性对照（同一个探针程序，只换 java.exe 的位置）：

| java.exe 位置 | 进程完整性级别 | `Files.isWritable` | 真实写入 | zipfs `isReadOnly` |
|---|---|---|---|---|
| `E:\titanmod\.tools\...`（工作区内） | **Low** | **false** | OK | **true → 失败** |
| `C:\dsh-tools\...`（工作区外） | **Medium** | **true** | OK | **false → 正常** |

结论：**完整性级别由「可执行文件是否位于工作区内」决定**，与 `danger-full-access` 无关。

## 六之二、两种可用方案（都已实测 BUILD SUCCESSFUL）

### 方案 A（当前配置，推荐日常用）

JDK 留在工作区 `E:\titanmod\.tools`，NeoForm 改用自带 ECJ：

```properties
neoForge.neoFormRuntime.verbose=true
neoForge.neoFormRuntime.useEclipseCompiler=true
```

- 已验证：`gradlew build` ✅、`gradlew runClient` ✅（游戏进主菜单，`mod/titanmod` 已加载）
- 优点：不用把 500MB 的 JDK 放到 C 盘；环境自包含 
- 缺点：编译器是 ECJ，不是 javac

### 方案 B（想用原生 javac）

JDK 放到工作区外，并关掉 ECJ：

```powershell
# JDK 已放到 C:\dsh-tools\jdk-21.0.12.1+1
$env:JAVA_HOME = 'C:\dsh-tools\jdk-21.0.12.1+1'
```

```properties
neoForge.neoFormRuntime.useEclipseCompiler=false
```

- 已验证：工作区调用的 `gradlew.bat` + 工作区外 JDK，`clean build` ✅ `BUILD SUCCESSFUL in 11s`
  （关键：**gradlew.bat 从哪调用无所谓，只要 `JAVA_HOME` 里的 java.exe 在工作区外**）
- 优点：走原生 javac，与技能文档默认口径一致
- 缺点：JDK 在 C 盘，换机器要重新放

两种方案产出同一个 `titanmod-1.0.0.jar`（6746 字节）。


## 七、已解决的问题：runClient 原生崩溃（AMD 显卡驱动）

### 现象

`gradlew build` 成功后，`gradlew runClient` 在启动早期崩溃：

```
#  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x00007ffd4a1a3ca0
# Problematic frame:
# C  [atio6axx.dll+0x193ca0]
j  org.lwjgl.glfw.GLFW.nglfwCreateWindow(IIJJJ)J
j  net.neoforged.fml.loading.ModDirTransformerDiscoverer.earlyInitialization(...)
```

**这是显卡驱动的原生崩溃，与 mod 代码无关。**

### 本机显卡情况

| 显卡 | 驱动版本 | 驱动日期 |
|---|---|---|
| NVIDIA GeForce RTX 3060 Laptop | 32.0.16.1047 | 2026-05-19 |
| **AMD Radeon(TM) Graphics（集显）** | 31.0.12024.2005 | **2022-09-06（4 年未更新）** |
| OrayIddDriver Device（远程虚拟显示器） | 17.1.58.818 | 2022-11-15 |

崩溃发生在 `atio6axx.dll`，即集显的 OpenGL 驱动。
系统日志另有多次 `Display driver nvlddmkm stopped responding and has successfully recovered`（TDR 复位）。

### 修复：把跑游戏的 java 进程强制指向 RTX 3060

已写入当前用户的显卡首选项（与「设置 → 系统 → 显示 → 图形」里手动添加是同一处）：

```powershell
$key = 'HKCU:\Software\Microsoft\DirectX\UserGpuPreferences'
New-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\java.exe'  -Value 'GpuPreference=2;' -PropertyType String -Force
New-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\javaw.exe' -Value 'GpuPreference=2;' -PropertyType String -Force
```

`GpuPreference=2` = 高性能（独显）。改完 `runClient` → **BUILD SUCCESSFUL**，
游戏窗口正常打开、进到主菜单，日志关键行：

```
[Render thread/INFO] [minecraft/Minecraft]: Setting user: Dev
[Render thread/INFO] [minecraft/Minecraft]: Backend library: LWJGL version 3.3.3+5
[modloading-worker-0/INFO] [ne.ne.ne.co.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.252, for MC 1.21.1
Titan Mod (NeoForge 1.21.1) 1.0.0 (titanmod)
[Render thread/INFO] [minecraft/ReloadableResourceManager]: Reloading ResourceManager: vanilla, mod_resources, mod/titanmod, mod/neoforge
[Render thread/INFO] [mojang/Library]: OpenAL initialized on device OpenAL Soft on 扬声器 (C-Media(R) Audio)
[Render thread/INFO] [minecraft/Minecraft]: Stopping!
BUILD SUCCESSFUL
```

注意日志里 `mod/titanmod` 出现在资源管理器列表中——**模组已被游戏加载**。

> 换 JDK 路径或换机器后这条显卡首选项会失效，需要按新路径重新设置。
> 如果独显也不稳，可考虑更新 AMD 集显驱动（2022 年的版本明显过旧）。


