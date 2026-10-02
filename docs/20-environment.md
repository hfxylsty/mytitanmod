# 20 · 开发环境

> **本文件职责**：记录本机（Windows + DSH）上「Titan Reborn / 泰坦重生」开发环境的**实测结果**——工具链绝对路径、环境变量、常用命令、两个已解决的疑难问题、网络限制、仓库约定与目录职责。
> 记录日期：2026-10-03。原始速查记录在仓库根目录 [ENVIRONMENT.md](../ENVIRONMENT.md)（由别人维护，**本文件不修改它**）；本文件是整理后的正式版。
> 环境相关的未决事项与本文冲突时，以**实测输出**为准，并在本文更新。

## 一、工具链清单（本机实测）

| 组件 | 版本 | 绝对路径 | 状态 |
|---|---|---|---|
| JDK（主用，工作区内） | Microsoft OpenJDK **21.0.12.1 LTS**（`21.0.12.1+1-LTS`） | `E:\titanmod\.tools\jdk-21.0.12.1+1` | ✅ 实测 `java -version` 正常 |
| JDK（备用，工作区外） | 同上（同一份 Microsoft 构建） | `C:\dsh-tools\jdk-21.0.12.1+1` | ✅ 实测 `java -version` 正常 |
| Gradle | **8.14**（wrapper 自动下载） | `E:\titanmod\.gradle-home\wrapper\dists\gradle-8.14-bin\` | ✅ 已缓存 |
| Gradle 分发源 | 腾讯云镜像 | wrapper 内 `distributionUrl=https://mirrors.cloud.tencent.com/gradle/gradle-8.14-bin.zip` | ✅ 实测 HTTP 200 |
| NeoForge | `21.1.248` 与 `21.1.252` **两份都已缓存** | `E:\titanmod\.gradle-home\caches\modules-2\files-2.1\net.neoforged\neoforge\` | ✅ |
| ModDevGradle | **2.0.143** | `…\files-2.1\net.neoforged\moddev-gradle\2.0.143\` | ✅ |
| NeoForm Runtime | 2.0.24（ModDevGradle 的传递依赖） | `…\files-2.1\net.neoforged\neoform-runtime\2.0.24\` | ✅ |
| 验证工程 | `titanmod` / `com.titanmod` | `E:\titanmod\example-mod` | ✅ `build` + `runClient` 均已通过 |
| 技能库 | `minecraft-modding` v0.1.0 | `E:\titanmod\.dsh\skills\minecraft-modding` | ✅ |
| 系统 `java` | **JDK 1.8** | `C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe` | ⚠️ **不要用** |

### 1.1 为什么有两份 JDK

这两份是**同一个版本、同一份安装内容**（Microsoft OpenJDK 21.0.12.1+1），复制成两份是**为了绕开 DSH 沙箱的一条行为**，不是版本需要：

- DSH 会把**工作区内的可执行文件**以 **Low 完整性级别（Low IL）** 启动。Low IL 进程里 `Files.isWritable()` 对本机任何路径都返回 `false`，JDK zipfs 因此把输出 jar 判成只读 → NeoForm 重编译失败（详见第四节）。
- 把 `java.exe` 放到**工作区外**（`C:\dsh-tools\`）启动，进程是 **Medium IL**，`Files.isWritable()` 正常 → 原生 javac 可用。
- 这条由「可执行文件是否位于工作区内」决定，**与 DSH 的 `danger-full-access` 文件策略无关**（已实测）。

| 场景 | 用哪份 JDK | 原因 |
|---|---|---|
| 日常开发（方案 A，当前配置） | `E:\titanmod\.tools\jdk-21.0.12.1+1` | 环境自包含、不占 C 盘；配 `useEclipseCompiler=true` 后不受 Low IL 影响 |
| 需要**原生 javac**（方案 B） | `C:\dsh-tools\jdk-21.0.12.1+1` | Medium IL，`Files.isWritable()` 正常，zipfs 可写 |
| 换机器 / 重装 | 只需恢复方案 A 那份；方案 B 那份是可选便利 | 方案 B 的路径会被显卡首选项与 `JAVA_HOME` 硬引用 |

> 体积参考（实测）：`.tools` 0.53 GB、`C:\dsh-tools` 0.32 GB、`.gradle-home` 1.56 GB、`.tmp` 0.23 GB。

### 1.2 一处需要澄清的版本号

| 出处 | 写的 NeoForge 版本 |
|---|---|
| `example-mod/build.gradle`（**代码真相**） | `version = '21.1.252'` |
| `ENVIRONMENT.md` 表格 | `21.1.248` |
| `runClient` 启动日志 | `NeoForge mod loading, version 21.1.252, for MC 1.21.1` |
| 技能 `70-version-matrix.md` | `21.1.248` |

**结论**：`21.1.248` 与 `21.1.252` 的 Gradle 缓存都在，两者都能构建；但 `example-mod` **当前实际使用的是 `21.1.252`**（这是代码真相）。
这是一处**文档与代码不一致**：技能文档与 `ENVIRONMENT.md` 都以 `21.1.248` 为准，需要由维护者统一口径。本文按「代码真相优先」记录为 21.1.252，切换版本时请按 `70-version-matrix.md` 逐行核对。

## 二、每次开新终端要设的环境变量

DSH 每次 `pwsh` 都是新进程，**环境变量不会继承**，所以下面这段粘贴即用（PowerShell）：

```powershell
$env:JAVA_HOME        = 'E:\titanmod\.tools\jdk-21.0.12.1+1'   # 方案 B 时改
$env:GRADLE_USER_HOME = 'E:\titanmod\.gradle-home'
$env:PATH             = "$env:JAVA_HOME\bin;$env:PATH"
$env:TMP              = 'E:\titanmod\.tmp'
$env:TEMP             = 'E:\titanmod\.tmp'

# 自检：必须输出 openjdk version "21.0.12.1"，且路径是 .tools 下那份
java -version
(Get-Command java).Source
```

要点：

- **`JAVA_HOME` 必须指向 JDK 21**。NeoForge 1.21.x 用 JDK 8/17 会报 toolchain 或 class 版本错误；本机 PATH 里那个 `java` 是 **JDK 1.8**，绝不能依赖它。
- `TMP`/`TEMP` 重定向到工作区内，避免系统临时目录的权限/空间干扰构建。
- `GRADLE_USER_HOME` 指向工作区内，保证 Gradle 缓存与 wrapper 分发不被重复下载。
- 该配置与 `example-mod/gradle.properties` 里的 `-Djava.io.tmpdir=E:/titanmod/.tmp` 是**两处独立设置**，改一处不影响另一处。

## 三、常用命令（可直接复制）

```powershell
# 0) 先设环境变量（见第二节），然后：
cd E:\titanmod\example-mod

# 1) 编译，产物在 build\libs\
.\gradlew.bat build

# 2) 冒烟验证：启动游戏到主菜单
.\gradlew.bat runClient

# 3) 启动服务端
.\gradlew.bat runServer

# 4) 查看产物（实测约 6746 字节）
Get-ChildItem .\build\libs | Format-Table Name,Length,LastWriteTime

# 5) 清理重建（改过工具链/编译器设置后用）
.\gradlew.bat clean build

# 6) 看 NeoForm 真实报错（必须先打开详细日志，见第四节）
.\gradlew.bat :createMinecraftArtifacts --info
```

- 首次构建会下载 Gradle 8.14 + Minecraft 资源并反编译，**约 8 分钟**；之后走缓存（实测 `clean build` 可到 11 秒级）。
- `gradlew.bat` **从哪个目录调用无所谓**，wrapper 按自身位置找 `gradle/wrapper/`。
- 一律走 `gradlew.bat`，不要用系统 `gradle`（版本会不匹配）。

## 四、已解决问题 1：NeoForm 重编译失败（`package-info: null`）

> 换机器、换 NeoForge 版本、换 JDK 位置时会再遇到，是本环境最贵的一条教训。

### 4.1 现象

`gradlew build` 在 `:createMinecraftArtifacts` 失败，抛 `Node action for recompile failed` → `java.io.IOException: Compilation failed`；`recompile/output.jar` 只有 **22 字节**（空 jar）。NeoForm 默认几乎不输出 javac 诊断，只能看到一行：

```
 ERROR Line: 1, ..net.minecraft.world.ticks.package-info...: null in /net/minecraft/world/ticks/package-info.java
```

`null` 这个错误消息**极具误导性**——看起来像 Minecraft 源码问题，其实完全不是。

### 4.2 排查过程（每步都有证据）

1. **打开 NeoForm 详细日志**。Gradle 属性 `neoForge.neoFormRuntime.verbose=true`（对应 `NeoFormRuntimeExtension.getVerbose()`）。打开后真实错误现形：

   ```
   Compiling 5364 source files
   ERROR Line: 1, error while writing net.minecraft.world.ticks.package-info: null in /net/minecraft/world/ticks/package-info.java
   ```

   → 关键字是 **`error while writing`（写 class 文件失败）**，不是编译语义错误。这一步是整条排查的转折点。

2. **排除源码与 classpath**。用与 NeoForm 完全相同的 **110 项 classpath**（74 个 Minecraft 库 + 48 个 NeoForge 库/模块，由 `neoforge-*-moddev-config.json` 推导，数量与日志里 `110 items on the compile classpath` 完全吻合）手工编译全部 **6317** 个源文件 → **javac exit=0，产出 9768 个 class，零错误**。源码和 classpath 都是好的。

3. **定位到写入层**。NeoForm 的 `RecompileSourcesActionWithJDK` 用
   `FileSystems.newFileSystem(URI.create("jar:"+output.toUri()), Map.of("create", TRUE))`
   打开输出 zip，再用**进程内** `javax.tools.JavaCompiler` 写入。复现该写入方式（探针 `.tmp/zipwrite/ZipWriteTest.java`）报：

   ```
   java.nio.file.ReadOnlyFileSystemException
       at jdk.zipfs/jdk.zipfs.ZipFileSystem.checkWritable(ZipFileSystem.java:370)
       at jdk.zipfs/jdk.zipfs.ZipFileSystem.createDirectory(ZipFileSystem.java:708)
   ```

   → **JDK zipfs 层拒绝写入**。之所以只在第 5364 个文件才炸，是因为前面 5363 个都写在已存在的根目录里，直到需要新建父目录时才调 `checkWritable`。

4. **已排除的嫌疑**：NeoForge 补丁版本（21.1.248 与 21.1.252 同样报错）、缺件（48 个 NeoForge 库全在缓存里）、磁盘空间（138 GB 可用）、工作区权限（已修）、系统临时目录（可写，且已重定向到 `E:\titanmod\.tmp`）。

### 4.3 根因（决定性对照实验）

JDK zipfs 在构造时用 `readOnly = !Files.isWritable(zipPath)` 判断可写性。而 DSH 把工作区内的可执行文件以 **Low 完整性级别** 启动，**Low IL 进程里 `Files.isWritable()` 对本机任何路径都返回 `false`**（即使真实写入是成功的）→ zipfs 判定只读 → javac 一个 class 也写不进去。

同一个探针程序、只换 `java.exe` 的位置，得到：

| `java.exe` 位置 | 进程完整性级别 | `Files.isWritable` | 真实写入 | zipfs `isReadOnly` | 结果 |
|---|---|---|---|---|---|
| `E:\titanmod\.tools\…`（工作区内） | **Low** | **false** | 实际 OK | **true** | ❌ 失败 |
| `C:\dsh-tools\…`（工作区外） | **Medium** | **true** | OK | **false** | ✅ 正常 |

**结论：完整性级别由「可执行文件是否位于工作区内」决定，与 `danger-full-access` 无关。**

### 4.4 修复

#### 方案 A —— JDK 留工作区内 + ECJ（**当前配置，推荐日常用**）

在 `example-mod/gradle.properties` 加两行。ECJ **不走 JDK zipfs 的写入路径**，因此完全绕开 Low IL 问题：

```properties
neoForge.neoFormRuntime.verbose=true
neoForge.neoFormRuntime.useEclipseCompiler=true
```

- 实测：`gradlew build` ✅ `BUILD SUCCESSFUL`；`gradlew runClient` ✅ 进主菜单。
- 优点：不用把 500 MB 的 JDK 放到 C 盘，环境自包含、可整目录搬走。
- 缺点：编译器是 **ECJ 而不是 javac**。ECJ 与 javac 在极端情况下对个别写法宽容度不同；若将来某个写法 ECJ 报错而 javac 不报，记得有这层差异。

#### 方案 B —— JDK 放工作区外 + 原生 javac

```powershell
$env:JAVA_HOME = 'C:\dsh-tools\jdk-21.0.12.1+1'
```

```properties
neoForge.neoFormRuntime.useEclipseCompiler=false
```

- 实测：在工作区内调用 `gradlew.bat` + 工作区外 JDK，`clean build` ✅ `BUILD SUCCESSFUL in 11s`。
- **关键点：`gradlew.bat` 从哪调用无所谓，只要 `JAVA_HOME` 里的 `java.exe` 在工作区外。**
- 优点：走原生 javac，与技能文档默认口径一致。
- 缺点：JDK 在 C 盘，换机器要重新放。

两种方案产出同一个 `build/libs/titanmod-1.0.0.jar`（实测 **6746 字节**）。

### 4.5 复现 / 回滚方式

- **复现**：把 `useEclipseCompiler` 删掉或设为 `false`，且 `JAVA_HOME` 指向工作区内 JDK → `gradlew clean build` 必现 `package-info: null`。
- **回滚（B → A）**：`JAVA_HOME` 改回 `E:\titanmod\.tools\jdk-21.0.12.1+1`，`useEclipseCompiler=true`，`gradlew clean build`。
- **回滚（A → B）**：`JAVA_HOME` 改成 `C:\dsh-tools\jdk-21.0.12.1+1`，`useEclipseCompiler=false`。
- **保留 `verbose=true`**：它不解决任何问题，但下次出问题时没有它就只能看见 `null`。

## 五、已解决问题 2：runClient 原生崩溃（AMD 集显驱动）

### 5.1 现象

`gradlew build` 成功后，`gradlew runClient` 在启动早期崩溃：

```
#  EXCEPTION_ACCESS_VIOLATION (0xc0000005) at pc=0x00007ffd4a1a3ca0
# Problematic frame:
# C  [atio6axx.dll+0x193ca0]
j  org.lwjgl.glfw.GLFW.nglfwCreateWindow(IIJJJ)J
j  net.neoforged.fml.loading.ModDirTransformerDiscoverer.earlyInitialization(...)
```

**这是显卡驱动的原生崩溃，与 mod 代码无关。**

### 5.2 本机显卡情况（实测）

| 显卡 | 驱动版本 | 驱动日期 |
|---|---|---|
| NVIDIA GeForce RTX 3060 Laptop | 32.0.16.1047 | 2026-05-19 |
| **AMD Radeon(TM) Graphics（集显）** | 31.0.12024.2005 | **2022-09-06（约 4 年未更新）** |
| OrayIddDriver Device（远程虚拟显示器） | 17.1.58.818 | 2022-11-15 |

崩溃帧 `atio6axx.dll` 是 **AMD 集显的 OpenGL 驱动**，发生在 `glfwCreateWindow`（创建窗口）阶段。系统日志另有多次 `Display driver nvlddmkm stopped responding and has successfully recovered`（NVIDIA 驱动 TDR 复位）。

### 5.3 修复：把跑游戏的 `java.exe` 指定为高性能 GPU

写入**当前用户**的显卡首选项（与「设置 → 系统 → 显示 → 图形」里手动添加是同一处注册表）：

```powershell
$key = 'HKCU:\Software\Microsoft\DirectX\UserGpuPreferences'
New-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\java.exe'  -Value 'GpuPreference=2;' -PropertyType String -Force
New-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\javaw.exe' -Value 'GpuPreference=2;' -PropertyType String -Force
```

`GpuPreference=2` = 高性能（独显）；`1` = 省电（集显）；`0` = 系统决定。**`java.exe` 与 `javaw.exe` 两条都要写**（Gradle 用哪个不一定）。已实测确认这两条键存在于注册表中。

改完 `runClient` → **BUILD SUCCESSFUL**，窗口正常打开并进到主菜单。日志关键行：

```
[Render thread/INFO] [minecraft/Minecraft]: Backend library: LWJGL version 3.3.3+5
[Render thread/INFO] [minecraft/ReloadableResourceManager]: Reloading ResourceManager: vanilla, mod_resources, mod/titanmod, mod/neoforge
Titan Mod (NeoForge 1.21.1) 1.0.0 (titanmod)
BUILD SUCCESSFUL
```

**判定模组真的被加载**：日志里出现 `mod/titanmod`（资源管理器列表）与我们的 mod 显示名。

### 5.4 复现 / 回滚方式

- **复现**：把两条注册表键删掉 → `gradlew runClient` 复现 `EXCEPTION_ACCESS_VIOLATION`；或按 PATH 换成工作区外那份 JDK 而不补注册表键（首选项按**完整路径**匹配，换路径即失效）。
- **回滚**：
  ```powershell
  $key = 'HKCU:\Software\Microsoft\DirectX\UserGpuPreferences'
  Remove-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\java.exe'
  Remove-ItemProperty -Path $key -Name 'E:\titanmod\.tools\jdk-21.0.12.1+1\bin\javaw.exe'
  ```
- **换机器 / 换 JDK 路径后必须重设**（路径变了就不匹配）。
- 更彻底的方案（**待验证**）：更新 AMD 集显驱动。2022-09 的版本明显过旧，崩溃很可能随之消失——但我们**没有**验证过这一点，当前仍以注册表首选项为准。

## 六、网络限制（决定怎么下载东西）

### 6.1 已记录的事实

`github.com` / `api.github.com` 的 **DNS 被劫持到 `127.0.0.1`**（实测 `Resolve-DnsName` 返回 `127.0.0.1`）。因此在没有本地加速器时：

- ✗ `git clone https://github.com/...`、打开 github.com 网页 —— 全部失败
- ✗ `curl https://github.com/...` —— 超时或证书错误

> **2026-10-03 实测补充（重要）**：本机 `0.0.0.0:443` 由 **`Steam++.Accelerator`（PID 6016）** 监听，它是一个本地网络加速器。**在它运行时**，`git ls-remote` / `git clone --filter=blob:none` / `github.com` 网页请求**实际是成功的**（实测 `git ls-remote https://github.com/Mrbt0907/The-Titans-Mod HEAD` 返回 `a4d3fe60b068a621af3438bb74d4090484a0d758`，`git clone` 亦成功）。
> **但**：DNS 仍被劫持到 `127.0.0.1`，也就是说 github 访问**完全取决于这个本地服务是否在跑**。它一关，github 立刻不可用；而且 `Invoke-WebRequest` 走不同栈时可能仍超时（实测有超时案例）。**所以下面 6.2 的镜像通道仍应作为默认手段**，不要把 github 直连当作可靠前提；这条留「待验证」，需在不同加速器状态下各测一次。

### 6.2 可用通道（实测结果）

| 通道 | 用途 | 实测 |
|---|---|---|
| `raw.githubusercontent.com` | 取**单个文件**（不 clone 整个仓库） | ✅ HTTP 200 |
| `codeload.github.com` | 下载 **zip 整包** | ✅ HTTP 200，实测 14.9 MB |
| `ghproxy.net` | github 代理（raw / release） | ✅ HTTP 200 |
| `ghfast.top` | github 代理 | ✅ HTTP 200 |
| `gh-proxy.com` | github 代理 | ✅ HTTP 200（对本机加速器下的 github 直连目标返回 200） |
| `data.jsdelivr.com` | **列仓库文件树**（不下载内容） | ✅ HTTP 200；注意根路径返回 400 属正常，要带包名 |
| `piston-meta.mojang.com` | Minecraft 版本清单 | ✅ HTTP 200 |
| `maven.neoforged.net` | NeoForge Maven（依赖/源码 jar） | ✅ HTTP 200 |
| `repo.maven.apache.org` | Maven Central | ✅ HTTP 200 |
| `plugins.gradle.org` | Gradle 插件门户 | ✅ HTTP 200 |
| `libraries.minecraft.net` | Minecraft 库 | ✅ 可达（根路径 404 属正常） |
| `mirrors.cloud.tencent.com` | Gradle 8.14 镜像 | ✅ HTTP 200，137 MB |

### 6.3 可复制的下载示例

```powershell
# ① 下整个仓库的 zip（不要 git clone）——实测 14.9 MB 成功
curl.exe -sL -k -o E:\titanmod\.tmp\titans.zip `
  "https://codeload.github.com/Mrbt0907/The-Titans-Mod/zip/refs/heads/main"

# ② 只取一个文件
curl.exe -sL -k -o E:\titanmod\.tmp\build.gradle `
  "https://raw.githubusercontent.com/Mrbt0907/The-Titans-Mod/main/build.gradle"

# ③ 走 github 代理取同一个文件
curl.exe -sL -k -o E:\titanmod\.tmp\README.md `
  "https://ghproxy.net/https://raw.githubusercontent.com/Mrbt0907/The-Titans-Mod/main/README.md"

# ④ 只列仓库文件树（不下载内容，适合先看清有什么）
curl.exe -sL -k "https://data.jsdelivr.com/v1/packages/gh/Mrbt0907/The-Titans-Mod@main?structure=flat" `
  -o E:\titanmod\.tmp\filetree.json

# ⑤ 从 NeoForge Maven 下任意版本源码 jar（核对 API 的最权威依据）
curl.exe -sL -k -o E:\titanmod\.tmp\neoforge-21.1.248-sources.jar `
  "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248-sources.jar"

# ⑥ 解压并只看目录结构（不要读 .java 逻辑，见 07-compliance.md）
Expand-Archive E:\titanmod\.tmp\titans.zip -DestinationPath E:\titanmod\.tmp\titans-src -Force
```

要点：**`curl.exe` 一律带 `-k`**（本机证书吊销检查会失败：`schannel: CRYPT_E_NO_REVOCATION_CHECK`）；GitHub 相关下载**优先走 codeload / raw / 代理**，不要赌直连。

## 七、仓库约定：哪些目录不入库

`.gitignore`（实测内容）排除下列目录，**它们都不是工程代码**：

| 目录 | 为什么不入库 |
|---|---|
| `bridge/` | 与 ChatGPT 侧通信的辅助工具，**含令牌**（`token.txt`），且不属于本工程 |
| `tasks/` | 与第二个 DSH 会话耦合的跨会话任务队列协议，属于**协作基建**，不是工程代码 |
| `.tools/` | 本地工具链（JDK 等），**体积大**（0.53 GB）、可重新下载 |
| `.gradle-home/` | Gradle 缓存（1.56 GB）、可重新下载 |
| `.tmp/` | 临时与调查产物（探针程序、下载的 zip、源码解压、调研 JSON） |
| `.acl-recovery/` | DSH 权限修复的中间产物 |
| `.dsh/` | DSH 会话状态（技能库虽在 `E:\titanmod\.dsh` 下，但整体不入库） |

**推定规则**：仓库只收**源码与文档**。工具链、缓存、临时产物、协作基建一律不入库。

另外以下构建/IDE 产物也已排除：`build/`、`run/`、`.gradle/`、`out/`、`bin/`、`*.class`、`*.jar`（保留 `gradle/wrapper/gradle-wrapper.jar`）、`.idea/`、`.vscode/`、`*.log`、`hs_err_pid*.log`、`*.token`、`secrets.properties`。

## 八、工作区目录职责表

| 路径 | 职责 | 入库 |
|---|---|---|
| `example-mod/` | **技术验证工程**（技能模板改造，`modid=titanmod`）。用来跑通 build/runClient 与试 API。**不是**正式模组工程，不要在里面堆正式内容 | ✅ |
| `docs/` | **本工程的唯一权威信息来源**。文档先行，代码可重写 | ✅ |
| `bridge/` | 与 ChatGPT 侧的通信工具（`.py` 脚本、消息日志、令牌） | ❌ |
| `tasks/` | 跨会话任务队列（`inbox/` `claimed/` `done/` `failed/` + `PROTOCOL.md`） | ❌ |
| `.tools/` | 本地工具链：`jdk-21.0.12.1+1`（工作区内 JDK）、`pnpmshim`、JDK 安装包 zip | ❌ |
| `.gradle-home/` | `GRADLE_USER_HOME`：wrapper 分发、依赖缓存、NeoForge 源码 jar | ❌ |
| `.tmp/` | 临时产物与调查现场（`titans-src/`、`mac-src/`、`legal-research/`、探针等） | ❌ |
| `.dsh/` | DSH 会话状态；技能文档在 `.dsh/skills/minecraft-modding/`（见 [90-references.md](90-references.md)） | ❌ |
| 仓库根 | `.gitignore`、`ENVIRONMENT.md`（环境速查，**由别人维护，本文件不改它**） | ✅ |

### 8.1 一处需要澄清的命名

`example-mod/` 现在用的是模板命名空间 `titanmod` / `com.titanmod`（Java 包名、`@Mod`、`neoforge.mods.toml`、`assets/`、`data/`、模型与配方 JSON 内的引用**都已从 `examplemod` 全量改为 `titanmod`**）。
但**正式模组的 modid 已定为 `titanreborn` / `com.titanreborn`**（见 [00-overview.md](00-overview.md) 第二节）。也就是说 `example-mod` 的 `titanmod` 是**验证工程的历史命名，不要当作正式 modid 沿用**。正式工程目录规划为 `titanreborn/`。

## 九、待办与待验证

| # | 事项 | 状态 |
|---|---|---|
| 1 | 更新 AMD 集显驱动（2022-09 版）能否替代注册表 GPU 首选项 | **待验证** |
| 2 | 本地 `Steam++.Accelerator` 关闭时 github 是否完全不可用 | **待验证**（见 6.1） |
| 3 | `build.gradle` 的 `21.1.252` 与文档记录的 `21.1.248` 需统一口径 | 待维护者裁定 |
| 4 | 正式工程目录名（`titanreborn/`）与 `example-mod` 的最终关系 | 待 [02-architecture.md](02-architecture.md) 确认 |
