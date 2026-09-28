# 隅光 · 九宮切圖

<p align="center">
  <a href="README.en.md">English</a> |
  <a href="README.de.md">Deutsch</a> |
  <a href="README.md">简体中文</a> |
  <strong>繁體中文</strong> |
  <a href="README.fa.md">فارسی</a>
</p>

獨立原生 Android 用戶端，Google Play 應用程式 ID 為 `app.cornerlight.ninegrid`。使用 Kotlin、Jetpack Compose 和 Android 平台 API 實作，不依賴 WebView、瀏覽器版本或後端服務。

架構、資料流、權限矩陣、記憶體策略、測試分層和新手接手順序請參閱[整體方案設計](docs/architecture.md)。

> 本專案的原創原始碼以 [MIT 授權條款](LICENSE)開源。圖片處理完全在本機完成，應用程式本身不連網、無帳號、無廣告、無統計。歡迎在 [GitHub 儲存庫](https://github.com/yjd953/cornerlight-android)提交 Issue 與 Pull Request。

## 功能

- 透過 Android 系統相片選擇器或剪貼簿匯入圖片；只讀取使用者主動選取的圖片，不要求完整相簿的讀取權限。
- 解碼 JPG、PNG、WebP 以及裝置支援的 HEIC/HEIF，並修正 EXIF 方向。
- 支援四宮格、九宮格、十二宮格，以及填滿畫面和保留完整圖片兩種模式。
- 拖曳調整主體位置、設定 0–12% 白邊、背景顏色和 60–100% JPEG 畫質。
- 使用 360 px 輕量預覽；正式匯出時逐張產生 1080 × 1080 JPEG，降低記憶體峰值。
- 可預覽、儲存或分享單張切圖；儲存或分享全部切圖；也可匯出完整 ZIP。
- Android 10 以上透過 `MediaStore` 寫入 `Pictures/隅光`，不需要儲存空間權限。
- Android 7–9 只在儲存時要求舊版寫入權限；選圖和 ZIP 匯出不需要此權限。
- 中英文介面切換、深淺色主題、Edge-to-Edge 版面，以及橫向和大螢幕捲動支援。

## 專案結構

```text
app/src/main/java/com/ninegrid/app/
├── core/
│   ├── image/       # 與介面無關的網格幾何和 Bitmap 繪製
│   └── model/       # 版式、裁切和編輯狀態模型
├── data/
│   ├── image/       # ContentResolver 解碼、容量/像素限制和 EXIF
│   ├── export/      # MediaStore、FileProvider、ZIP 和系統分享
│   └── preferences/ # 輕量主題與語言偏好
├── ui/
│   ├── components/  # 可重複使用的 Compose 元件
│   ├── editor/      # 單向資料流、ViewModel 和頁面
│   └── theme/       # Material 3 視覺權杖
├── AppContainer.kt  # 小型專案使用的明確相依性容器
└── MainActivity.kt  # Activity Result 與 Compose 進入點
```

專案採用單向資料流：介面送出使用者意圖，`NineGridViewModel` 更新不可變狀態，圖片與檔案工作交由 `core` 和 `data` 層處理。應用程式沒有導入反射式相依性注入、資料庫或網路層。

## 環境需求

- JDK 17
- Android SDK 36
- Android Studio 2025.2.1 或更新版本
- 最低 Android 7（API 24），目標 Android 16（API 36）

首次匯入後，Android Studio 會透過 Wrapper 下載 Gradle 和宣告的相依套件。`local.properties` 只儲存本機 SDK 路徑，不會提交到版本庫。

## 建置與檢查

```bash
# 單元測試
./gradlew testDebugUnitTest

# Android 靜態檢查
./gradlew lintDebug

# 建置儀器測試 APK
./gradlew assembleDebugAndroidTest

# 在已連線的裝置或模擬器執行 Bitmap、檔案和 Compose 測試
./gradlew connectedDebugAndroidTest

# Debug APK
./gradlew assembleDebug

# 正式簽署發布 AAB，需要先設定上傳金鑰
./gradlew bundleRelease
```

Debug APK 位於：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 發布簽章

Google Play App Signing 保存真正用來簽署使用者安裝套件的 App signing key。開發者在本機產生並保管 Upload key，用它簽署上傳至 Play Console 的 AAB。完整說明請參閱 [Google Play 發布準備](docs/google-play-release.md)。

請勿提交 `.jks`、`.keystore` 或密碼。將下列屬性放入使用者目錄的 `~/.gradle/gradle.properties`，或使用同名環境變數：

```properties
CORNERLIGHT_KEYSTORE_FILE=/absolute/path/to/cornerlight-upload.jks
CORNERLIGHT_KEYSTORE_PASSWORD=replace-me
CORNERLIGHT_KEY_ALIAS=cornerlight-upload
CORNERLIGHT_KEY_PASSWORD=replace-me
```

設定後執行：

```bash
./gradlew bundleRelease
```

用於 Play 商店發布的 AAB 位於 `app/build/outputs/bundle/release/`。
明確執行 `bundleRelease` 時，缺少任何簽章設定都會導致建置失敗；發布時請勿以 `bundle`、`assemble` 或 `build` 等彙總工作取代。

## 隱私與權限

- 應用程式不宣告網路權限，不會自行上傳圖片或傳送統計資料。
- 系統相片選擇器只授權單一選取 URI。
- 匯入的圖片會短暫複製到應用程式快取中進行解碼，正常處理完成後即刪除；預覽和分享檔案也只保存在應用程式快取中。
- 使用者主動呼叫系統分享時，所選的接收應用程式會取得相關切圖的暫時讀取權限，後續處理由該接收應用程式負責。
- 儲存的切圖只會在使用者主動操作後寫入公開相簿；ZIP 位置由使用者透過系統檔案選擇器決定。
- Android 9 以下的 `WRITE_EXTERNAL_STORAGE` 宣告帶有 `maxSdkVersion=28`，新系統不會要求此權限。
- 完整內容請參閱目前以中文維護的[隱私權政策](docs/privacy-policy.md)。

## 驗證範圍

CI 和命令列檢查可以涵蓋編譯、Lint、單元測試、APK/AAB 結構與簽章設定。發布前仍應在至少一台 Android 10 以上真機和一台低記憶體裝置上，驗證相片選擇器、不同廠商相簿、微信/小紅書分享入口及低記憶體行為。

## 參與貢獻

歡迎透過 Issue 回報問題、提出功能建議，或直接提交 Pull Request。提交前請確認 `./gradlew testDebugUnitTest lintDebug` 通過，並遵循現有程式碼風格與單向資料流分層。

## 開源授權

本專案的原創原始碼和非品牌文件依據 [MIT License](LICENSE) 發布。第三方元件繼續適用各自的授權條款，詳見[第三方授權說明](THIRD_PARTY_NOTICES.md)。

「隅光」「Cornerlight」名稱、應用程式圖示、Logo 與商店宣傳素材不在 MIT 授權範圍內，未經許可不得作為其他應用程式或專案的品牌使用。公開散布修改版或衍生版本時，必須更換名稱、圖示和應用程式 ID。完整界線請參閱[授權範圍](LICENSING.md)與[品牌政策](TRADEMARKS.md)。
