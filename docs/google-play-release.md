# 隅光 Google Play 发布准备

## 1. 应用身份

- 商店名称：`隅光 · 九宫切图`
- 手机桌面名称：`隅光`
- Google Play 应用 ID：`app.cornerlight.ninegrid`
- 当前版本：`versionCode 3`、`versionName 1.0.2`（以 `app/build.gradle.kts` 为准）

应用 ID 在首次上传后不能用于另一个应用，也不应再修改。创建 Play Console 应用时先确认该 ID 可以注册。

## 2. 两把密钥分别做什么

启用 Google Play App Signing 后，Google 保存 App signing key，并用它给最终分发到用户设备的 APK 签名。开发者在本机保存 Upload key，用它签署上传到 Play Console 的 AAB。二者不是从 Google 下载的“包”。

Upload key 不能公开或提交到代码仓库。现代 RSA 密钥无法通过“撞包名”或穷举现实地推导出来，主要风险是密钥文件和密码同时泄露，或 Play Console 账号被盗。启用 Play App Signing 后，即使 Upload key 泄露，也可以通过 Play Console 申请重置 Upload key；App signing key 仍由 Google 保护。

## 3. 本机生成 Upload key

在项目根目录运行：

```bash
./scripts/create-upload-keystore.sh
```

脚本只调用 JDK 自带的 `keytool`，密码由 `keytool` 在终端中交互询问，不会写进脚本或终端命令历史。建议：

1. 使用独立强密码，不复用 Google 账号密码。
2. 将密钥文件和密码分别备份到两个安全位置。
3. 不通过聊天、邮件或网盘公开分享密钥。
4. 为 Google 账号启用两步验证。

## 4. 配置签名

把下面四项写入本机 `~/.gradle/gradle.properties`，不要写进项目目录：

```properties
CORNERLIGHT_KEYSTORE_FILE=/absolute/path/to/cornerlight-upload.jks
CORNERLIGHT_KEYSTORE_PASSWORD=你的密钥库密码
CORNERLIGHT_KEY_ALIAS=cornerlight-upload
CORNERLIGHT_KEY_PASSWORD=你的密钥密码
```

然后生成 AAB：

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebugAndroidTest bundleRelease
```

输出文件：

```text
app/build/outputs/bundle/release/app-release.aab
```

显式执行 `assembleRelease` 或 `bundleRelease` 时，工程会在签名配置不完整的情况下终止构建。发布时应使用上面的 `bundleRelease` 命令，不要用 `assemble`、`bundle` 或 `build` 等聚合任务代替。

## 5. 隐私政策无需自有域名

只为 Google Play 提供隐私政策时，不必购买自有域名，也不必另建一个部署在中国大陆的网站。可以直接把项目内准备好的 `docs/privacy-policy.html` 发布到境外平台提供的公共子域名：

- Google Sites：使用 `sites.google.com` 的公开页面，不显示个人网站地址。
- Cloudflare Pages：使用随机或品牌化的 `pages.dev` 地址，不需要自有域名。

如果以后改用中国大陆境内的服务器或网站接入服务，应按当时适用的备案规则向服务商确认，不要把这里的境外托管方案理解成对所有网站场景的备案结论。

页面必须无需登录即可访问、不能要求权限、不能使用 PDF，并保持与 App 内说明及 Play Console Data safety 声明一致。建议先创建一个只使用品牌名称的支持邮箱，并把它填入 Play Console 的“应用支持”区域。

个人 Play Console 账号的法定姓名、国家/地区和开发者联系信息仍可能按 Google 政策显示；应用名称和包名本身不包含个人信息，不能替代或绕过 Google 的账号身份披露要求。
