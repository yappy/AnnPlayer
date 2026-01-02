# AnnPlayer

[![Android CI](https://github.com/yappy/AnnPlayer/actions/workflows/android.yml/badge.svg)](https://github.com/yappy/AnnPlayer/actions/workflows/android.yml)
[![Release](https://github.com/yappy/AnnPlayer/actions/workflows/release.yml/badge.svg)](https://github.com/yappy/AnnPlayer/actions/workflows/release.yml)

[Technical Note](./technote.md)

## 自動ビルド

Github に変更を push すると(ブランチを問わず) GitHub Actions でビルドが走ります。

タグを push するとビルドの後、さらにパッケージを Github releases のところに
自動でアップロードします。

リリースページ:
<https://github.com/yappy/AnnPlayer/releases>

## 罠

### パッケージがインストールできない

<https://stackoverflow.com/questions/25274296/adb-install-fails-with-install-failed-test-only>

`adb install app-release.apk` とすると `INSTALL_FAILED_TEST_ONLY` で失敗している。

Android Studio 3.0 以降、メニューの Run で生成した apk には android:testOnly がつき、
これが原因でインストールに失敗してしまうらしい。
Make や Build APK(s)、Generate Signed APK(s) など、それ以外の方法ならば問題ない。
(同じ場所に同じファイル名で違うファイルができる...)
一旦 app/build/ を丸ごと消してからやり直すとよい。
