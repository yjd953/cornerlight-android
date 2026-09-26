#!/usr/bin/env bash
set -euo pipefail

if ! command -v keytool >/dev/null 2>&1; then
  echo "未找到 keytool。请先安装 JDK 17，再重新运行。"
  exit 1
fi

default_path="$HOME/.android/keystores/cornerlight-upload.jks"
read -r -p "上传密钥保存路径（直接回车使用 $default_path）: " requested_path
keystore_path="${requested_path:-$default_path}"

if [[ -e "$keystore_path" ]]; then
  echo "目标文件已经存在，已停止，避免覆盖现有密钥：$keystore_path"
  exit 1
fi

mkdir -p -- "$(dirname -- "$keystore_path")"
umask 077

keytool -genkeypair -v \
  -keystore "$keystore_path" \
  -alias "cornerlight-upload" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

chmod 600 "$keystore_path"
echo
echo "上传密钥已生成：$keystore_path"
echo "请立即做离线备份，并按 docs/google-play-release.md 配置 Gradle。"

