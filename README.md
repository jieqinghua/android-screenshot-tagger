# 截图标记（ScreenshotTagger）

系统截图后自动把文件改名为带应用名的形式：

```
Screenshot_20261101_143005.png  →  Screenshot_20261101_143005_微信.png
```

适用于未 Root、Android 11+ 的手机，自用侧载安装。

## 工作原理

1. **无障碍服务**只订阅"窗口切换"事件，记录每个时刻的前台应用（不读取任何屏幕内容）。
2. **图库监听**：图库插入新图片时，判断是否在 `Screenshots` 目录；是的话按截图时间查出当时的前台应用，然后改名。
3. 系统界面、截图浮窗、输入法、Toast 不会被当作截图对象。

## 省电设计

- 无前台服务、无唤醒锁、无定时器/轮询/JobScheduler，完全事件驱动，空闲时不消耗 CPU。
- 后台线程只在处理截图时创建，空闲 30 秒自动退出。
- 不依赖 AndroidX / Compose / 协程，release 包开启 R8 压缩，常驻内存更小。

## 在线编译（推荐，无需安装任何工具）

1. 在 GitHub 新建仓库（Private 即可），把本文件夹内**所有文件**（包括隐藏的 `.github` 文件夹）上传。
   Finder 里按 `Cmd + Shift + .` 可显示隐藏文件。
2. 上传后自动开始编译（仓库 **Actions** 页可看进度，约 3–5 分钟），同时会跑单元测试。
3. 编译成功后，在仓库右侧 **Releases** 下载 `ScreenshotTagger.apk`；手机浏览器登录 GitHub 后直接打开下载安装。
   最新版固定链接：`https://github.com/<用户名>/<仓库名>/releases/latest/download/ScreenshotTagger.apk`

## 本地构建

本机需要 Android SDK 与 JDK 17。最简单的方式是安装 [Android Studio](https://developer.android.com/studio)，然后 **File → Open** 打开本目录，等待 Gradle 同步完成（会自动生成 Gradle Wrapper）。

命令行构建（同步后）：

```bash
./gradlew test
```

```bash
./gradlew assembleRelease
```

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

release 包用 debug 密钥签名，直接可装；日常使用建议装 release（体积和内存更小）。

## 首次使用

1. 打开"截图标记"，依次点"去设置"：开启**无障碍服务**、授予**所有文件访问**、忽略**电池优化**。
2. 国产 ROM 额外在系统设置中打开本应用的**自启动**，电池策略设为**无限制**。
3. （可选）让服务被系统关闭后开机自动恢复，并支持在应用内一键开启无障碍：

```bash
adb shell pm grant com.example.screenshottagger android.permission.WRITE_SECURE_SETTINGS
```

## 验证

```bash
adb logcat -s ScreenshotTagger
```

打开任意应用截图，约 2 秒后日志出现 `renamed ... -> ..._应用名.png`，应用内"最近改名记录"同步显示。

资源占用检查：

```bash
adb shell dumpsys meminfo com.example.screenshottagger
```

```bash
adb shell dumpsys batterystats com.example.screenshottagger
```
