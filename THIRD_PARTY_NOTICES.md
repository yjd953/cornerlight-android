# 第三方许可说明

隅光使用通过 Gradle 获取的第三方开源组件。这些组件不受本项目 MIT License 的重新授权，仍适用各自的许可证。

## 随应用分发的主要组件

| 项目或组件系列 | 用途 | 许可证 |
| --- | --- | --- |
| AndroidX Core、Activity、Lifecycle、ExifInterface | Android 平台兼容、生命周期与图片方向处理 | Apache License 2.0 |
| Jetpack Compose、Material 3、Material Icons | 界面、布局与图标 | Apache License 2.0 |
| Kotlin Standard Library | Kotlin 运行时 | Apache License 2.0 |
| Kotlinx Coroutines | 协程与异步任务 | Apache License 2.0 |
| JetBrains Annotations、JSpecify、ListenableFuture | 传递依赖中的注解与兼容接口 | Apache License 2.0 |

准确版本以 [`gradle/libs.versions.toml`](gradle/libs.versions.toml) 和 Gradle 实际解析结果为准。测试依赖不会打入正式应用包。

Apache License 2.0 正文位于 [`app/src/main/assets/legal/Apache-2.0.txt`](app/src/main/assets/legal/Apache-2.0.txt)，该文件会随 APK/AAB 一起分发。
