# 32 · Blockbench 安装记录（2026-10-04）

> 本文件职责：记录为项目配置**可视化建模工具**的过程与结论，避免重复踩坑。

## 一、结论（先看这个）

| 方式 | 状态 | 说明 |
|---|---|---|
| **Blockbench 网页版** `https://web.blockbench.net/` | ✅ **推荐，可直接用** | 官方提供，同一套代码，插件照装。**不需要安装任何东西** |
| 桌面版（E 盘 / C 盘） | ❌ **本会话内装不起来** | 见下方原因；安装包已留好，可在你自己的终端装 |

**为什么桌面版装不起来**：安装器解包出来的应用**不完整**，缺少 Electron 必需的
`icudtl.dat` / `resources.pak` / `locales/`，且 `resources/app.asar` 头部不是有效的
asar 签名（应为 `{"files":{`）。启动报错：

```
ERROR:base\i18n\icu_util.cc:232] Invalid file descriptor to ICU data received.
退出码 -2147483645 (0x80000003)
```

排除过的可能性（都验证过**不是**原因）：
- ❌ 文件损坏 —— 两个安装包的**数字签名均有效**（签发者 `CN=Jannis Tobias Petersen`，即 Blockbench 作者）
- ❌ 下载不完整 —— 字节数与 GitHub 声明**完全一致**（portable 97002768、installer 107986096）
- ❌ E 盘写权限 —— E 盘可正常写入、可建深层嵌套目录、可复制 exe
- ❌ 路径太长 / 中文路径 —— 换到 `E:\bb`、C 盘短路径均同样失败
- ❌ stdio 管道 —— 改用 `Start-Process`、文件重定向、`explorer` 启动，均同样失败

**推测原因**：本会话的沙箱环境对 Electron 类应用的初始化有限制（与之前
[JDK 的 Low 完整性级别问题](20-environment.md) 同类的环境限制）。

## 二、已下载并保留的安装包（可供你在自己终端安装）

两个包都放在 `E:\titanmod\.tools\blockbench\`，**数字签名已验证有效**：

| 文件 | 大小 | 用途 |
|---|---|---|
| `Blockbench_x64_setup.exe` | 103 MB | 正式安装版（推荐，会装到 `%LOCALAPPDATA%\Programs\Blockbench`） |
| `Blockbench_5.2.1_portable.exe` | 92.5 MB | 便携版（NSIS 自解压，每次启动解到临时目录） |

**你自己终端里的安装步骤**（不受本会话沙箱限制）：
1. 双击 `Blockbench_x64_setup.exe` 按提示安装（免费开源，无需付费/破解）
2. 打开 Blockbench → 菜单 `File` → `Plugins...`
3. 在 `Available` 标签搜 **`GeckoLib Models & Animations`** → 点 `Install`

## 三、GeckoLib 插件（已替你下好）

| 项 | 值 |
|---|---|
| 插件名 | **GeckoLib Models & Animations** |
| 版本 | **4.2.5** |
| 适用 | Blockbench **5.x**（`min_version: 5.0.0`；注意旧的 "GeckoLib Animation Utils" 只适用于 4.x，已被弃用） |
| 文件位置 | `E:\titanmod\.tools\blockbench-plugin\geckolib.js`（425 KB）+ `icon.png` |

**网页版装载插件的方法**：把 `geckolib.js` 拖进 Blockbench 窗口，或用
`File` → `Plugins` → `Load Plugin from File` 选择它。

**桌面版**：把 `geckolib.js` 放进 `<Blockbench 用户数据目录>\plugins\geckolib\` 即可，
用户数据目录通常是 `%APPDATA%\Blockbench`（本会话已验证该目录会被自动创建）。

## 四、下载渠道（本机网络下可用）

`github.com` 网页与 `api.github.com` 被 DNS 劫持，**必须走镜像**：

```powershell
# 查最新版本
curl.exe -sL "https://gh-proxy.com/https://api.github.com/repos/JannisX11/blockbench/releases/latest"

# 下载（ghfast.top 与 gh-proxy.com 均实测可用）
curl.exe -sL -o bb.exe "https://ghfast.top/https://github.com/JannisX11/blockbench/releases/download/v5.2.1/Blockbench_x64_5.2.1.exe"

# 插件（Blockbench 官方插件索引仓库）
curl.exe -sL -o geckolib.js "https://gh-proxy.com/https://raw.githubusercontent.com/JannisX11/blockbench-plugins/master/plugins/geckolib/geckolib.js"
```

**校验下载完整性的办法**（比看大小更可靠）：

```powershell
Get-AuthenticodeSignature <文件路径> | Select-Object Status, SignerCertificate
# Status 应为 Valid
```

> ⚠️ 注意：**签名有效只保证外层安装器完整**，不等于内部解包一定成功
> ——我们遇到的正是"签名有效但解包不完整"。

## 五、用 Blockbench 打开本项目的模型

本项目的 Blockbench 工程文件在
[`design/titan_zombie/`](../design/titan_zombie/)：

| 文件 | 说明 |
|---|---|
| `titan_zombie.geo.json` | 模型（Bedrock 几何，游戏实际读取） |
| `titan_zombie.png` | 贴图 256×256（含原版铁剑贴图） |
| `titan_zombie.bbmodel` | Blockbench 工程（编辑用，需自己另存） |
| `README.md` | **操作说明（先看这个）** |


改完模型后导出覆盖这三个同名文件即可，**Java 代码不用动**：
- `assets/titanreborn/geo/entity/titan_zombie.geo.json`
- `assets/titanreborn/animations/entity/titan_zombie.animation.json`
- `assets/titanreborn/textures/entity/titan_zombie.png`
