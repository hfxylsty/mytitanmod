# M1 验收自动检查（A1~A11，编号对应 docs/06-roadmap.md §3.2）
#
# 用法（在仓库根或任意位置）：
#   pwsh -File tools\verify-m1.ps1                    # 全跑：构建 + 静态核对 + 进游戏跑一遍
#   pwsh -File tools\verify-m1.ps1 -SkipClient        # 只跑构建与静态核对（约 1 分钟）
#   pwsh -File tools\verify-m1.ps1 -SourceWorld 新的世界   # 指定用来复制验证世界的存档
#
# 说明：
# - 「进游戏」那一段用 runClientVerify 这个 run（见 build.gradle）：它会自动进入
#   run/saves/titanverify 这个**每次重新复制**的验证世界。世界里的数据包
#   titanverify:setup 负责摆好场景（平台、玩家位置、刷一只带 tv_target 标签的泰坦）。
# - 截图落在 titanreborn/run/screenshots/，日志断言读 titanreborn/run/logs/latest.log。
# - 判定为「人工待验」的项目，脚本不会假装通过；请配合截图人工确认。
#
# 退出码：0 = 所有自动项通过；1 = 有自动项未通过；2 = 脚本自身出错。

param(
    [string]$SourceWorld = '',
    [switch]$SkipClient,
    [int]$Shots = 10
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$proj = Join-Path $root 'titanreborn'
$run  = Join-Path $proj 'run'
$log  = Join-Path $run 'logs\latest.log'

$env:JAVA_HOME        = Join-Path $root '.tools\jdk-21.0.12.1+1'
$env:GRADLE_USER_HOME = Join-Path $root '.gradle-home'
$env:PATH             = "$env:JAVA_HOME\bin;$env:PATH"
$env:TMP              = Join-Path $root '.tmp'
$env:TEMP             = $env:TMP

$results = New-Object System.Collections.ArrayList
function Add-Result($id, $name, $status, $evidence) {
    [void]$results.Add([pscustomobject]@{ Id = $id; Name = $name; Status = $status; Evidence = $evidence })
    Write-Host ("[{0}] {1}  {2}" -f $id, $status, $name)
}

function Invoke-Gradle([string[]]$Arguments, [string]$OutFile) {
    $out = & (Join-Path $proj 'gradlew.bat') @Arguments 2>&1 | Out-String
    if ($OutFile) { $out | Set-Content $OutFile -Encoding UTF8 }
    return $out
}

# ---------------------------------------------------------------- A1 构建
Write-Host '=== A1 构建 ==='
$buildOut = Invoke-Gradle @('build', '--console=plain') (Join-Path $env:TMP 'verify-build.log')
$jar = Get-ChildItem (Join-Path $proj 'build\libs') -Filter 'titanreborn-*.jar' -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($buildOut -match 'BUILD SUCCESSFUL' -and $jar) {
    Add-Result 'A1' '构建通过' '✅ 通过' ("BUILD SUCCESSFUL；产物 $($jar.Name) ($($jar.Length) 字节)")
} else {
    Add-Result 'A1' '构建通过' '❌ 未通过' 'gradlew build 未成功或没有 jar 产物，见 .tmp/verify-build.log'
}

# ------------------------------------------------- 静态核对（支撑 A4/A5/A6）
Write-Host '=== 静态核对：模型 / 动画 / 碰撞箱 ==='
$assets = Join-Path $proj 'src\main\resources\assets\titanreborn'
$geo  = Get-Content (Join-Path $assets 'geo\entity\titan_zombie.geo.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$anim = Get-Content (Join-Path $assets 'animations\entity\titan_zombie.animation.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$g = $geo.'minecraft:geometry'[0]
$bones = @{}; $cubeCount = 0
$minX = 1e9; $maxX = -1e9; $minY = 1e9; $maxY = -1e9
foreach ($b in $g.bones) {
    $bones[$b.name] = $b
    foreach ($c in $b.cubes) {
        $cubeCount++
        $minX = [Math]::Min($minX, $c.origin[0]); $maxX = [Math]::Max($maxX, $c.origin[0] + $c.size[0])
        $minY = [Math]::Min($minY, $c.origin[1]); $maxY = [Math]::Max($maxY, $c.origin[1] + $c.size[1])
    }
}
$wBlocks = [Math]::Round(($maxX - $minX) / 16, 3); $hBlocks = [Math]::Round(($maxY - $minY) / 16, 3)
$entitiesSrc = Get-Content (Join-Path $proj 'src\main\java\com\titanreborn\ModEntities.java') -Raw -Encoding UTF8
$sizedOk = $entitiesSrc -match 'sized\(16\.0F,\s*32\.0F\)'
$requiredBones = @('head','torso_0','torso_1','torso_2','right_arm','left_arm','right_leg','left_leg',
                   'bone_plate_0','bone_plate_1','bone_plate_2','right_arm_lower')
$missing = @($requiredBones | Where-Object { -not $bones.ContainsKey($_) })
$animNames = @('animation.titan_zombie.idle','animation.titan_zombie.walk','animation.titan_zombie.attack')
$missingAnim = @($animNames | Where-Object { -not $anim.animations.PSObject.Properties.Name.Contains($_) })

$staticNotes = @()
$staticNotes += "模型包围盒 ${wBlocks} x ${hBlocks} 格（骨骼 $($g.bones.Count) / 立方体 $cubeCount）"
$staticNotes += if ($sizedOk -and $wBlocks -eq 16 -and $hBlocks -eq 32) { '碰撞箱 sized(16,32) 与模型一致 ✅' } else { '碰撞箱与模型不一致 ❌' }
$staticNotes += if ($missing.Count -eq 0) { '代码/规格依赖的骨骼名齐全 ✅' } else { "缺骨骼：$($missing -join ',') ❌" }
$staticNotes += if ($missingAnim.Count -eq 0) { "三个动画名齐全（idle $($anim.animations.'animation.titan_zombie.idle'.animation_length)s / walk $($anim.animations.'animation.titan_zombie.walk'.animation_length)s / attack $($anim.animations.'animation.titan_zombie.attack'.animation_length)s）✅" } else { "缺动画：$($missingAnim -join ',') ❌" }
Write-Host ($staticNotes -join "`n")

# 部件命中盒 vs 模型对位（Entity.setPos 语义 = 包围盒底面中心）
$partLines = @()
$partSpec = @(
    @{n='head';         w=8.0;  h=8.0; y=28.0},
    @{n='chest';        w=9.0;  h=9.0; y=16.25},
    @{n='bone_plate_0'; w=1.75; h=3.0; y=19.5},
    @{n='bone_plate_1'; w=1.75; h=3.0; y=18.625},
    @{n='bone_plate_2'; w=1.75; h=3.0; y=19.5}
)
foreach ($p in $partSpec) {
    $b = $bones[$p.n]
    if (-not $b -or $p.n -eq 'chest') { continue }
    $bminY = 1e9; $bmaxY = -1e9
    foreach ($c in $b.cubes) { $bminY = [Math]::Min($bminY, $c.origin[1]); $bmaxY = [Math]::Max($bmaxY, $c.origin[1] + $c.size[1]) }
    $partLines += ("{0}: 部件 y {1}~{2} 格 / 模型 y {3}~{4} 格" -f $p.n, $p.y, ($p.y + $p.h), [Math]::Round($bminY/16,3), [Math]::Round($bmaxY/16,3))
}
$chestBox = $bones['torso_0']
Write-Host ($partLines -join "`n")

Add-Result 'A4' '实体可生成（静态部分）' '👁 人工待验' ("脚本会进世界 /summon 并截图；模型尺寸与碰撞箱：" + $staticNotes[1])
Add-Result 'A5' '多部件命中（静态部分）' '👁 人工待验' ("5 个部件名与模型骨骼对位情况：" + ($partLines -join '；'))
Add-Result 'A6' '破防生效（静态部分）' '👁 人工待验' '伤害规则已实现「未破防归零 / 伤害上限 / 最小门槛」；破防窗口与阶段破防未实现，需游戏内验证'

# ---------------------------------------------------------------- A10 数据
Write-Host '=== A10 数据文件 ==='
$dataRoot = Join-Path $proj 'src\main\resources\data\titanreborn'
$loot = @(); $recipe = @(); $tag = @()
if (Test-Path $dataRoot) {
    $loot   = @(Get-ChildItem $dataRoot -Recurse -File -Filter *.json | Where-Object { $_.FullName -match '\\loot_table\\' })
    $recipe = @(Get-ChildItem $dataRoot -Recurse -File -Filter *.json | Where-Object { $_.FullName -match '\\recipe\\' })
    $tag    = @(Get-ChildItem $dataRoot -Recurse -File -Filter *.json | Where-Object { $_.FullName -match '\\tags\\' })
}
if ($loot.Count -gt 0 -and $recipe.Count -gt 0) {
    Add-Result 'A10' '数据生效' '✅ 通过' ("掉落表 $($loot.Count) 份、配方 $($recipe.Count) 份、标签 $($tag.Count) 份")
} else {
    Add-Result 'A10' '数据生效' '⬜ 未实现' ("data/titanreborn 下：掉落表 $($loot.Count) 份、配方 $($recipe.Count) 份 —— M1 §3.1 要求各 1 份，尚未做")
}

# ---------------------------------------------------------------- A7/A9 未实现项
$srcAll = Get-ChildItem (Join-Path $proj 'src\main\java') -Recurse -File -Filter *.java | ForEach-Object { Get-Content $_.FullName -Raw -Encoding UTF8 }
$srcText = $srcAll -join "`n"
if ($srcText -match 'regrow|Regrow|重生长') {
    Add-Result 'A7' '重生长触发' '👁 人工待验' '代码里出现重生长相关标识，需进游戏降到阈值以下观察'
} else {
    Add-Result 'A7' '重生长触发' '⬜ 未实现' '源码中没有重生长（缩小/无敌/恢复）逻辑'
}
if ($srcText -match 'minion|Minion|忠诚者|loyalist') {
    Add-Result 'A9' '召唤物' '👁 人工待验' '代码里出现召唤物相关标识，需进游戏使用召唤道具验证'
} else {
    Add-Result 'A9' '召唤物' '⬜ 未实现' '没有一级召唤物（忠诚者）与其召唤道具'
}
Add-Result 'A8' '免疫生效' '👁 人工待验' ("源码：摔落免疫={0}、流体免疫={1}；火焰/虚空未单独处理，且伤害规则会把未破防伤害一律归零，游戏内无法区分" -f ($srcText -match 'causeFallDamage[\s\S]{0,120}return false'), ($srcText -match 'isAffectedByFluids[\s\S]{0,60}return false'))

# ---------------------------------------------------------------- A11 专用服务器
Write-Host '=== A11 专用服务器 ==='
$eula = Join-Path $run 'server\eula.txt'
if (Test-Path $eula) {
    $eulaText = Get-Content $eula -Raw
    if ($eulaText -match 'eula\s*=\s*true') {
        Add-Result 'A11' '专用服务器' '👁 人工待验' 'eula.txt 已同意；本脚本未自动跑 runServer，请单独执行 gradlew runServer 验收'
    } else {
        Add-Result 'A11' '专用服务器' '⛔ 阻塞' 'eula.txt 存在但未同意，需用户确认 EULA'
    }
} else {
    Add-Result 'A11' '专用服务器' '⛔ 阻塞' 'run/server/eula.txt 不存在：接受 Mojang EULA 需用户本人确认（见 docs/06-roadmap.md §3.4）'
}

# ---------------------------------------------------------------- A2/A3 客户端
if ($SkipClient) {
    Add-Result 'A2' '客户端启动' '⏭ 跳过' '指定了 -SkipClient'
    Add-Result 'A3' '模组加载' '⏭ 跳过' '指定了 -SkipClient'
} else {
    Write-Host '=== A2/A3 客户端启动（约 3~4 分钟）==='
    $saves = Join-Path $run 'saves'
    if (-not $SourceWorld) {
        $cand = Get-ChildItem $saves -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -ne 'titanverify' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if (-not $cand) { throw "run/saves 下没有可用存档，请用 -SourceWorld 指定" }
        $SourceWorld = $cand.Name
    }
    $verifyWorld = Join-Path $saves 'titanverify'
    if (Test-Path $verifyWorld) { Remove-Item -Recurse -Force $verifyWorld }
    Copy-Item -Recurse -Force (Join-Path $saves $SourceWorld) $verifyWorld
    Write-Host "验证世界：由存档「$SourceWorld」复制到 titanverify"

    # 数据包（必须无 BOM，否则第一行命令解析失败）
    $dp = Join-Path $verifyWorld 'datapacks\titanverify'
    New-Item -ItemType Directory -Force -Path (Join-Path $dp 'data\minecraft\tags\function'), (Join-Path $dp 'data\titanverify\function') | Out-Null
    $enc = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText((Join-Path $dp 'pack.mcmeta'), '{"pack":{"pack_format":48,"description":"titan M1 verification datapack"}}', $enc)
    [IO.File]::WriteAllText((Join-Path $dp 'data\minecraft\tags\function\tick.json'), '{"values":["titanverify:tick"]}', $enc)
    [IO.File]::WriteAllText((Join-Path $dp 'data\titanverify\function\tick.mcfunction'), "execute as @a[tag=!tv_done] at @s run function titanverify:setup`n", $enc)
    $setup = @"
tag @s add tv_done
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule doImmediateRespawn true
time set noon
weather clear
effect give @s minecraft:resistance 99999 4 true
effect give @s minecraft:regeneration 99999 4 true
effect give @s minecraft:saturation 99999 4 true
tp @s ~ 200 ~ 0 18
fill ~-12 ~-1 ~-12 ~12 ~-1 ~48 minecraft:stone
execute unless entity @e[type=titanreborn:titan_zombie,tag=tv_target] run summon titanreborn:titan_zombie ~ ~ ~30 {Tags:["tv_target"]}
gamemode survival @s
"@
    [IO.File]::WriteAllText((Join-Path $dp 'data\titanverify\function\setup.mcfunction'), $setup, $enc)

    if (Test-Path $log) { Remove-Item $log -Force }
    $stdout = Join-Path $env:TMP 'verify-client.log'
    $stderr = Join-Path $env:TMP 'verify-client.err'
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue
    $proc = Start-Process -FilePath (Join-Path $proj 'gradlew.bat') -ArgumentList 'runClientVerify', '--console=plain' `
            -WorkingDirectory $proj -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $startedAt = Get-Date

    Add-Type -TypeDefinition @'
using System; using System.Text; using System.Runtime.InteropServices;
public class VW {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetClassName(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
  public static IntPtr Find() {
    IntPtr f = IntPtr.Zero;
    EnumWindows((h, l) => { if (!IsWindowVisible(h)) return true;
      var sb = new StringBuilder(64); GetClassName(h, sb, 64);
      if (sb.ToString().StartsWith("GLFW")) { f = h; return false; } return true; }, IntPtr.Zero);
    return f;
  }
}
'@
    $hwnd = [IntPtr]::Zero
    for ($i = 0; $i -lt 100 -and $hwnd -eq [IntPtr]::Zero; $i++) {
        Start-Sleep -Seconds 2
        if ($proc.HasExited) { break }
        $hwnd = [VW]::Find()
    }
    Write-Host "游戏窗口：$hwnd（启动后 $([int]((Get-Date) - $startedAt).TotalSeconds)s）"
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        if ($proc.HasExited) { break }
        $t = Get-Content $log -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
        if ($t -match 'Loaded \d+ recipes') { break }
    }
    Start-Sleep -Seconds 12
    for ($i = 1; $i -le $Shots; $i++) {
        if ($proc.HasExited) { break }
        $hwnd = [VW]::Find(); if ($hwnd -eq [IntPtr]::Zero) { break }
        [void][VW]::PostMessage($hwnd, 0x0100, [IntPtr]0x71, [IntPtr](1 -bor (0x3C -shl 16)))   # F2 down
        Start-Sleep -Milliseconds 60
        [void][VW]::PostMessage($hwnd, 0x0101, [IntPtr]0x71, [IntPtr](0xC0000001 -bor (0x3C -shl 16)))
        Start-Sleep -Seconds 4
    }
    Start-Sleep -Seconds 2
    if (-not $proc.HasExited) { taskkill /PID $proc.Id /T /F 2>&1 | Out-Null }
    Start-Sleep -Seconds 2
    Get-Process java -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -like "$env:JAVA_HOME*" } |
        ForEach-Object { try { $_.Kill() } catch {} }

    $t = Get-Content $log -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    $shots = @(Get-ChildItem (Join-Path $run 'screenshots') -Filter *.png -ErrorAction SilentlyContinue |
               Where-Object { $_.LastWriteTime -gt $startedAt })
    if ($t -match 'mod/titanreborn' -and $t -match 'Loaded \d+ recipes' -and $t -notmatch 'FATAL|CrashReport') {
        Add-Result 'A2' '客户端启动' '✅ 通过' ("进世界成功；截图 $($shots.Count) 张；日志无 FATAL/CrashReport")
    } else {
        Add-Result 'A2' '客户端启动' '❌ 未通过' "未在日志里同时看到 mod/titanreborn 与世界加载，见 .tmp/verify-client.log"
    }
    if ($t -match 'Titan Reborn\s+([\d.]+)\s+\(titanreborn\)') {
        Add-Result 'A3' '模组加载' '✅ 通过' ("日志：Titan Reborn " + $Matches[1] + " (titanreborn)")
    } else {
        Add-Result 'A3' '模组加载' '❌ 未通过' '日志里没有 mod 名与版本行'
    }

    # A4 生成
    $summoned = ($t -split "`n" | Where-Object { $_ -match 'Summoned new|已生成' } | Select-Object -First 1)
    if ($summoned) {
        Add-Result 'A4' '实体可生成' '✅ 自动部分通过 / 👁 目视待验' ("日志：$($summoned.Trim())；是否可见/会动请看 run/screenshots 本轮截图")
    } else {
        Add-Result 'A4' '实体可生成' '❌ 未通过' '日志里没有 summon 成功记录'
    }

    # A5 部件命中
    $hitLines = @($t -split "`n" | Where-Object { $_ -match '命中部件=' })
    $partNames = @($hitLines | ForEach-Object { if ($_ -match '命中部件=(\S+)') { $Matches[1] } } | Select-Object -Unique)
    if ($partNames.Count -ge 2) {
        Add-Result 'A5' '多部件命中' '✅ 通过' ("命中日志 $($hitLines.Count) 条，区分出部件：" + ($partNames -join ','))
    } elseif ($hitLines.Count -eq 1) {
        Add-Result 'A5' '多部件命中' '⚠ 部分通过' ("只命中 1 个部件：$($partNames -join ',')")
    } else {
        Add-Result 'A5' '多部件命中' '👁 人工待验' '本轮自动流程没有打出部件命中（玩家与泰坦距离/朝向不合适）；需人工近身攻击'
    }

    # A6 破防
    $dmg = @($t -split "`n" | Where-Object { $_ -match '\[M0\.5\]' })
    $zero = @($dmg | Where-Object { $_ -match '最终=0\.0' })
    $real = @($dmg | Where-Object { $_ -match '最终=([1-9]\d*(\.\d+)?)' })
    if ($zero.Count -gt 0 -and $real.Count -gt 0) {
        Add-Result 'A6' '破防生效' '✅ 通过' ("未破防归零 $($zero.Count) 条，破防后实伤 $($real.Count) 条")
    } elseif ($zero.Count -gt 0) {
        Add-Result 'A6' '破防生效' '⚠ 部分通过' ("未破防归零 $($zero.Count) 条；没有「击碎 3 片甲片后造成实伤」的记录（破防窗口尚未实现）")
    } else {
        Add-Result 'A6' '破防生效' '👁 人工待验' '本轮没有产生伤害事件'
    }
    Write-Host ("本轮截图：" + (($shots | Sort-Object LastWriteTime | ForEach-Object { $_.Name }) -join ', '))
}

# ---------------------------------------------------------------- 汇总
Write-Host ''
Write-Host '================ M1 验收汇总（A1~A11）================'
$results | Sort-Object Id | Format-Table -AutoSize -Wrap | Out-String -Width 160 | Write-Host
$pass = @($results | Where-Object { $_.Status -match '通过' -and $_.Status -notmatch '未通过|部分' }).Count
$fail = @($results | Where-Object { $_.Status -match '未通过' }).Count
Write-Host "自动项通过 $pass 项，未通过 $fail 项；「未实现 / 人工待验 / 阻塞」见上表状态列。"

$summaryPath = Join-Path $env:TMP 'verify-m1-summary.txt'
$results | Sort-Object Id | Format-Table -AutoSize -Wrap | Out-String -Width 160 | Set-Content $summaryPath -Encoding UTF8
Write-Host "汇总已写入 $summaryPath"

if ($fail -gt 0) { exit 1 } else { exit 0 }
