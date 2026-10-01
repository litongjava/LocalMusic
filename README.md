# LocalMusic 本地音乐

离线音乐播放器：多目录授权、子目录合集、搜索卡片首页、黑胶播放页、系统媒体控制。

## 使用

1. 安装 `app/build/outputs/apk/debug/app-debug.apk`（Android 6.0 及以上）。
2. 打开「我的 → 右上角 ⋮ → 设置 → 添加音乐目录」，在系统文件选择器中授权一个本地目录。可重复添加多个目录。
3. 每个直接子目录成为一个合集；更深层音频归入这个合集。根目录直接包含的音频归入以所选文件夹名命名的合集（例如 CallRecording），不使用完整存储路径。
4. 应用启动时扫描；复制或移走文件后，可点首页刷新或「重新扫描全部目录」。
5. 搜索匹配歌曲文件名、合集名或本地标签，不区分大小写。同名但路径不同的合集分别显示；重叠目录中的同一文件只收录一次。

示例：

```text
Music/                         ← 在设置中选择这个目录
├── Fool's garden/             ← 合集名称
│   ├── Lemon Tree.mp3
│   └── Disc 2/Another.flac    ← 仍归入 Fool's garden
└── 夜间电台/
    └── Episode 01.m4a
```

授权由 Android 持久保存。移除目录只撤销访问、移出索引，不删除原文件。失效或无法读取的目录会在设置中提示，其他目录继续扫描。系统可能不允许选择存储根目录、Download 根目录或 Android/data；请选择其允许授权的音乐子目录。

## 播放

- 播放/暂停、拖动进度、上一首/下一首、队列选曲。
- 点击模式按钮依次切换：列表循环 → 单曲循环 → 随机播放 → 顺序播放。
- 定时停止：15 / 30 / 60 分钟；可取消。
- Media3 `MediaSessionService` 管理播放，Activity 销毁不释放播放器。
- 系统通知栏、锁屏、媒体按键共享同一个 MediaSession；音频焦点交由 ExoPlayer 管理，耳机断开时暂停。
- 应用未声明网络访问权限；目录选择器仅显示本地来源。

播放页采用全屏渐变、黑胶唱片与矢量控件；收起播放器后可返回原页面，底部迷你播放器继续工作。有内嵌封面时显示实际封面，无封面时显示原创本地封面。

## 歌词、封面与外部音频

- 点击唱片进入歌词页，点击「返回封面」返回唱片。播放时唱针落到唱片沟槽，暂停时抬起。
- 优先读取已导入的歌词，其次读取同目录同名 `.lrc`，再读取 MP3 ID3v2.3/2.4 的 USLT 内嵌歌词。SAF 目录需要已授权。支持 UTF-8、UTF-16 和 GB18030 文本。
- LRC 时间轴随播放高亮、滚动，点击歌词行跳转；纯文本歌词正常展示。可在歌词页选择文件导入，保存在应用内部；不联网获取歌词。导入限制 1 MB。
- 封面优先使用音频中的内嵌图片；没有时显示无文字默认图。当前内置 Lemon Tree MP3 没有内嵌封面；用户提供的同目录 LRC 已作为 `Lemon Tree.lrc` 一起打包，可离线显示中英歌词。
- 文件管理器「打开方式」和「分享」支持音频，包括 OGG。授权的文件会复制到应用内部「打开的音频」合集，保留原文件，避免临时 URI 授权结束后无法播放。每个文件限制 1 GB；卸载应用会移除该副本。

## 语言与图标

应用名称、界面、提示与无障碍标签默认跟随系统：中文显示「本地音乐」，英文及其他语言回退显示「Local Music」。底部英文导航为 Home / Albums / Notes / My。歌曲名、目录名和用户输入的歌单、笔记保持原文。图标为红色背景、白色唱片音符，支持自适应圆形图标。

## 我的、歌单与收藏

「我的」保留本地音乐、收藏和笔记，右上角 ⋮ 打开设置，不包含登录、会员、关注等联网功能。

- 点右上角 ＋ 或「新建歌单」创建歌单；支持重命名、删除、选歌添加和移除歌曲。
- 播放页「加入歌单」可添加当前歌曲；合集详情右上角 ⋮ 菜单可把整个合集加入歌单。相同歌曲不会重复添加。
- 播放页爱心收藏单曲；合集详情右上角 ⋮ →「收藏此目录合集」收藏音乐根目录下的二级目录合集。
- 「我的 → 收藏」分别查看歌曲与目录合集。目录暂不可用时保留收藏并提示检查授权。
- 删除歌单、取消收藏不会删除音乐文件。用户数据保存在本机 SQLite 数据库，重启与重新扫描不会清空。

## 播放笔记

播放页点击笔记图标或「写笔记」，打开编辑框时记录当前歌曲与时间点，音乐可继续播放。支持保存、编辑、删除；从底部「笔记」或「我的 → 笔记」查看，点「播放此处」回到对应歌曲的时间点。播放页「查看笔记」只列出当前歌曲的笔记。

笔记与歌单保存歌曲快照；原文件移动或目录权限取消后，笔记仍保留，恢复原目录授权后才能继续播放该音频。卸载应用会清除本机用户数据。

进程被系统彻底终止后，不自动恢复队列或自动播放。蓝牙硬件、来电及厂商省电策略仍需要对应真机环境验证。

## 内置音频

`app/src/offline/assets/music/Fool's garden/Lemon Tree.mp3` 来自用户提供的本机 MP3，原样打包；无需网络或存储授权。新增内置音乐可按 `assets/music/合集/文件` 放置后重新构建。发布安装包前应确认相应音频的分发授权。

## 工程结构

- `app/src/offline/`：当前应用源码、Manifest、界面资源、内置音频。
- `app/src/offlineTest/`：索引、资源、目录状态和界面生命周期测试。
- `app/src/offlineAndroidTest/`：真机解码、暂停、seek、切歌、实际单曲循环、顺序结束、列表循环和系统媒体控制测试。
- `app/src/main/`：保留原项目的页面、ExoPlayer 单例及 Whisper 实验代码，当前构建不打包这些旧代码和模型。

当前入口仍为 `com.litongjava.localmusic.MainActivity`，应用包名保持不变。新旧源码通过 `app/build.gradle` 的 sourceSets 隔离。

## 本机构建

使用 JDK 17 或 21、Gradle 8.11.1、Android Gradle Plugin 8.9.2、Android SDK 35。

`local.properties`（不提交）：

```properties
sdk.dir=D:/Android/sdk
```

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

真机测试会播放音频、切换到手机桌面并测试系统媒体会话，结束时暂停。需要已连接并授权的设备。

测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`；静态检查：`app/build/reports/lint-results-debug.html`。

### 本次验证（2026-10-01）

- 24 项本地自动化测试通过，涵盖扫描、去重、失效授权、内置音频、页面生命周期、歌单、收藏和笔记持久化。
- OnePlus CPH2583 / Android 16 真机 5 项集成测试通过，涵盖真实 MP3 解码、播放控制、循环、后台系统媒体会话，以及创建歌单、加歌、收藏单曲/目录、时间点笔记和设置入口的完整 UI 操作。
- 手动验证多个系统授权目录、深层音频归属、进程重启后授权保留、搜索、content URI 播放、熄屏播放与媒体键暂停/恢复。
- APK 构建和 Android lint 通过；内置 MP3 全曲解码通过。
- 蓝牙耳机实物、来电中断、15/30/60 分钟定时的完整等待与其他厂商机型未完成专项实测。

测试生成的歌单、笔记、收藏变更以及临时音频目录均已清理或恢复。

## 国内下载配置

根目录 `build.gradle` 的 **buildscript.repositories 和 allprojects.repositories** 均使用：

```groovy
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/public' }
}
```

Robolectric 会单独下载 Android 测试运行时，`app/build.gradle` 已设置：

```groovy
testOptions {
    unitTests.all {
        systemProperty 'robolectric.dependency.repo.url', 'https://maven.aliyun.com/repository/public'
    }
}
```

若全局 `gradle.properties` 配置了不可用代理，可注释其中 `systemProp.http.proxyHost/proxyPort`、`systemProp.https.proxyHost/proxyPort`，或仅对此次命令覆盖：

```powershell
.\gradlew.bat '-Dhttp.proxyHost=' '-Dhttps.proxyHost=' :app:assembleDebug
```

这些 Maven 配置不改变 Android Studio 的 SDK Manager 下载源。SDK Manager 使用 IDE 的 HTTP Proxy 设置；已有 `D:/Android/sdk` 足够构建本工程。

## 2026-10-01 本轮验证

- 32 项 JVM / Robolectric 测试通过：新增 LRC 时间轴、内嵌歌词解析、中英文资源、打开方式解析、音频与歌词持久保存。
- 6 项手机 instrumentation 测试通过：歌单、收藏、笔记、播放/暂停/跳转/循环、后台系统媒体控制；模拟独立文件管理器提供临时 content URI，撤销授权后副本仍播放，点击歌词行可跳转。
- `testDebugUnitTest`、`lintDebug`、`assembleDebug` 和 `assembleDebugAndroidTest` 通过。Lint 无错误，仍有旧工程及代码风格警告。
- 在英语系统手机确认 Local Music / My、播放唱针位置、无文字默认封面与歌词空态；系统 OGG 处理器查询包含本应用。中文与其他语言回退通过资源测试。

### 系统媒体封面修复

播放服务在后台读取内嵌图片；无图片时复用播放页的无文字默认封面，并写入 Media3 的 artworkData，由 MediaSession 发布给系统媒体中心、通知栏和锁屏。封面更新保留播放位置；切歌时丢弃过期加载结果。32 项单元测试、6 项真机测试与 Lint 通过，真机已校验系统媒体元数据中的 512×512 图片及控制中心封面。

合集详情保留紧凑概览卡片（最小高度 116dp），收藏、加入歌单和重新扫描位于右上角 ⋮ 菜单。已在真机确认 CallRecording 名称与菜单，并通过 32 项单元测试、合集收藏及歌单笔记真机流程测试和 Lint。

## 文件重命名与标签

- 长按歌曲或播放页 ⋮ →「重命名文件」：输入不带扩展名的新名称，保留原扩展名并修改实际文件名，不修改音频内容或内嵌 Title 标签。同名文件不会被覆盖；收藏、歌单、笔记和标签同步到新 URI，已有本地歌词保留在应用中。
- SAF 目录需要写入权限及文件提供方支持重命名。旧版添加的目录若只有读权限，请到「我的 → ⋮ → 设置」重新添加同一目录授权。APK 内置歌曲只读，不能改原文件名。
- 「编辑标签」可添加多个本地标签，用逗号分隔，最多 20 个，每个最多 40 字。清空保存即删除；可在首页或合集搜索框输入标签文字或 `#标签`。标签不写入原音频文件，卸载应用会清除。
- 数据库升级保留已有个人数据。35 项单元测试与 7 项真机测试通过，新增隔离 SAF 目录实际重命名、内容完整性、关联迁移和标签 UI 搜索验证。测试未重命名用户原有音频。

## 构建签名发行版

复制 `release-signing.properties.example` 为 `release-signing.properties`，填写自己的密钥库路径、别名与密码，再运行 `./gradlew :app:assembleRelease`。配置与密钥库均被 Git 忽略，应在本机安全备份；后续正式版使用同一签名才能覆盖升级。未配置签名时 Gradle 生成的 Release APK 不可直接安装。

GitHub v2.0.0 同时提供正式签名 APK、沿用本轮本机测试签名的兼容测试 APK 和 SHA-256 校验文件。正式版与测试版签名不同，不能覆盖安装；卸载会清除本机歌单、标签和笔记，请按 Release 说明选择安装包。
