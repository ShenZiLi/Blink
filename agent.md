# Agent 规则（Wink 项目）

## 编辑页 UI 规范
- **所有编辑/设置页的顶部导航栏统一为：左上角"返回"文字按钮，右上角"保存"文字按钮，标题居中。**
- 不许使用导航图标（如箭头图标），统一使用 `TextButton` + 文字。
- 左侧文字：`stringResource(R.string.edit_back)`
- 右侧文字：`stringResource(R.string.edit_save)`
- 布局方式：`Row` 包裹三个元素（左按钮、居中标题、右按钮），`Modifier.weight(1f)` 让标题撑满中间。

## Git 提交约定
- **每次功能改动完成后，立即将改动提交到本地 git。**（commit 到本地仓库，无需推送远端）
- 提交信息用中文、简洁描述本次改动内容与目的。
- 提交前先 `git add` 本次改动涉及的具体文件，避免误提交无关文件或敏感文件（如密钥）。
- 除非用户明确要求，否则不主动 push 到远端。

## 真机运行流程

**每次改动完成后，若当前已连接真实 Android 设备，则按以下流程构建、安装并启动 App，验证本次改动在真机上的表现。** 若仅模拟器或无线设备，则跳过运行步骤并说明原因。

### 全流程命令

```bash
# 1. 检测设备连接（adb 可能不在 PATH，需从 SDK 目录查找）
export PATH="$PATH:$HOME/.local/share/mise/installs/android-sdk/21.0/platform-tools"
adb devices

# 输出中应出现类似 "90d3e7c6        device" 的行。若无 device 状态设备则跳过后续步骤。

# 2. 构建 + 安装
GDIR=~/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle
# 先编译确保无错误
"$GDIR" assembleDebug --console=plain
# 安装到设备
"$GDIR" installDebug --console=plain

# 期望输出 "Installed on 1 device." 和 "BUILD SUCCESSFUL"

# 3. 拉起 App 确认不崩溃
adb shell monkey -p com.wink.eye -c android.intent.category.LAUNCHER 1

# 4. 验证进程存活
adb shell pidof com.wink.eye
# 有输出（如 28402）表示进程存活，无输出表示已崩溃

# 5. 可选：确认前台 Activity
adb shell "dumpsys activity activities | grep -i 'com.wink.eye' | head -3"
# 期望看到 topResumedActivity=com.wink.eye/.MainActivity
```

### 说明
- **adb 位置**：SDK 路径由 `local.properties` 中的 `sdk.dir` 决定，当前为 `$HOME/.local/share/mise/installs/android-sdk/21.0/platform-tools/adb`。若 PATH 无 adb，需先通过 `export PATH` 添加。
- **Gradle 位置**：当前项目 Gradle wrapper 下载的 dist 缓存于 `~/.gradle/wrapper/dists/gradle-8.13-bin/…/gradle-8.13/bin/gradle`，上述 `GDIR` 变量指向该路径。由于 Gradle wrapper 下载被 TLS 证书阻断，暂用该已解压的 `<GDIR>` 替代 `./gradlew`。
- **构建通过后可复用缓存**：后续增量构建通常只需几秒。