# 隅光 · 九宫切图

<p align="center">
  <a href="README.en.md">English</a> |
  <a href="README.de.md">Deutsch</a> |
  <strong>简体中文</strong> |
  <a href="README.zh-TW.md">繁體中文</a> |
  <a href="README.fa.md">فارسی</a>
</p>

独立原生 Android 客户端，Google Play 应用 ID 为 `app.cornerlight.ninegrid`。使用 Kotlin、Jetpack Compose 和 Android 平台 API 实现，不依赖 WebView、浏览器版或后端服务。

架构、数据流、权限矩阵、内存策略、测试分层和新人接手顺序见 [总体方案设计](docs/architecture.md)。

> 本项目的原创源代码以 [MIT 许可证](LICENSE) 开源。图片处理完全在本机完成，应用自身不联网、无账号、无广告、无统计。欢迎在 [GitHub 仓库](https://github.com/yjd953/cornerlight-android) 提交 Issue 与 Pull Request。

## 功能

- Android 系统照片选择器与剪贴板图片导入；只读取用户主动选择的照片，不申请整个相册读取权限。
- JPG、PNG、WebP 及设备支持的 HEIC/HEIF 解码，包含 EXIF 方向纠正。
- 四宫格、九宫格、十二宫格，支持裁满画面和保留全图。
- 拖动调整主体位置、0–12% 白边、背景颜色、60–100% JPEG 清晰度。
- 360px 轻量预览；正式导出逐张生成 1080×1080 JPEG，避免高内存峰值。
- 单张预览、单张保存/分享、全部保存、全部分享和 ZIP 导出。
- Android 10+ 使用 `MediaStore` 写入 `Pictures/隅光`，无需存储权限。
- Android 7–9 仅在保存时申请旧版写入权限；选图和 ZIP 导出不需要该权限。
- 中英文界面切换、深浅色主题、边到边布局、横竖屏和大屏自适应滚动。

## 工程结构

```text
app/src/main/java/com/ninegrid/app/
├── core/
│   ├── image/       # 与界面无关的网格几何和 Bitmap 渲染
│   └── model/       # 版式、裁剪和编辑状态模型
├── data/
│   ├── image/       # ContentResolver 解码、体积/像素限制和 EXIF
│   ├── export/      # MediaStore、FileProvider、ZIP 和系统分享
│   └── preferences/ # 轻量主题与语言偏好
├── ui/
│   ├── components/  # 可复用 Compose 组件
│   ├── editor/      # 单向数据流、ViewModel 和页面
│   └── theme/       # Material 3 视觉令牌
├── AppContainer.kt  # 小项目使用的显式依赖容器
└── MainActivity.kt  # Activity Result 与 Compose 入口
```

工程采用单向数据流：界面发出用户意图，`NineGridViewModel` 更新不可变状态，图片与文件工作交给 `core/data` 层。没有为小项目引入反射式 DI、数据库或网络层。

## 环境

- JDK 17
- Android SDK 36
- Android Studio 2025.2.1 或更新版本
- 最低 Android 7（API 24），目标 Android 16（API 36）

首次导入后，Android Studio 会按 Wrapper 下载 Gradle 和声明的依赖。`local.properties` 只保存本机 SDK 路径，不提交版本库。

## 构建与检查

```bash
# 单元测试
./gradlew testDebugUnitTest

# Android 静态检查
./gradlew lintDebug

# 编译仪器测试包
./gradlew assembleDebugAndroidTest

# 在已连接设备或模拟器上执行 Bitmap、文件和 Compose 测试
./gradlew connectedDebugAndroidTest

# 调试 APK
./gradlew assembleDebug

# 正式签名发布 AAB（需先配置上传密钥）
./gradlew bundleRelease
```

调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 发布签名

Google Play App Signing 保存真正给用户安装包签名的 App signing key；开发者在本机生成并保管 Upload key，用它给上传的 AAB 签名。完整说明见 [Google Play 发布准备](docs/google-play-release.md)。

不要提交 `.jks`、`.keystore` 或密码。将以下属性放入用户目录的 `~/.gradle/gradle.properties`，或者使用同名环境变量：

```properties
CORNERLIGHT_KEYSTORE_FILE=/absolute/path/to/cornerlight-upload.jks
CORNERLIGHT_KEYSTORE_PASSWORD=replace-me
CORNERLIGHT_KEY_ALIAS=cornerlight-upload
CORNERLIGHT_KEY_PASSWORD=replace-me
```

配置后执行：

```bash
./gradlew bundleRelease
```

用于商店发布的 AAB 位于 `app/build/outputs/bundle/release/`。
显式执行 `bundleRelease` 时，缺少任意签名项都会导致构建失败；发布时不要用 `bundle`、`assemble` 或 `build` 等聚合任务代替。

## 隐私与权限

- 应用不声明网络权限，不会自行上传图片或发送统计数据。
- 选图由系统照片选择器授权单个 URI。
- 导入图片会短暂复制到应用缓存用于解码，正常流程会在解码结束后删除；预览与分享文件也只保存在应用缓存目录。
- 用户主动调用系统分享时，所选接收应用会获得对应切图的临时读取权限；后续处理由该接收应用负责。
- 保存的成片由用户主动触发，写入公开相册；ZIP 位置由用户通过系统文件选择器决定。
- Android 9 及以下声明的 `WRITE_EXTERNAL_STORAGE` 带有 `maxSdkVersion=28`，不会在新系统申请。
- 可公开托管的完整文本见 [隐私政策](docs/privacy-policy.md)。

## 验证边界

CI/命令行可以覆盖编译、Lint、单元测试、APK/AAB 结构与签名配置。照片选择器、不同厂商相册、微信/小红书分享入口仍应在至少一台 Android 10+ 真机和一台低内存设备上完成发布前验收。

## 参与贡献

欢迎通过 Issue 反馈问题、提出功能建议，或直接提交 Pull Request。提交前请确保 `./gradlew testDebugUnitTest lintDebug` 通过，并遵循现有代码风格与单向数据流分层。

## 开源许可证

本项目原创源代码和非品牌文档基于 [MIT License](LICENSE) 发布。第三方组件继续适用各自的许可证，详见 [第三方许可说明](THIRD_PARTY_NOTICES.md)。

“隅光”“Cornerlight”名称、应用图标、Logo 与商店宣传素材不在 MIT 授权范围内，未经许可不得作为其他应用或项目的品牌使用。分发修改版或衍生版时必须更换名称、图标和应用 ID，完整边界见 [许可范围](LICENSING.md) 与 [品牌政策](TRADEMARKS.md)。
