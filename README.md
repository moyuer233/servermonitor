<div align="center">

# 服务器监控 (ServerMonitor)

_✨ 用 SSH 远程监控 Android / Linux 主机的运行状态，并能启停服务 ✨_

</div>

<p align="center">
  <img src="https://img.shields.io/github/license/qwqZYLqwq/servermonitor" alt="license">
  <img src="https://img.shields.io/github/v/release/qwqZYLqwq/servermonitor" alt="release">
  <img src="https://img.shields.io/github/downloads/qwqZYLqwq/servermonitor/total" alt="downloads">
  <img src="https://img.shields.io/github/commit-activity/m/qwqZYLqwq/servermonitor" alt="commit activity">
  <img src="https://img.shields.io/github/last-commit/qwqZYLqwq/servermonitor" alt="last commit">
  <br>
  <img src="https://img.shields.io/badge/platform-Android-3DDC84" alt="platform">
  <img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF" alt="Kotlin">
</p>

一款 Android 服务器监控应用：通过 SSH 连接远程服务器，实时查看 CPU / GPU / 内存 / 温度 / 电池等运行状态，
并可直接远程启动、停止、重启服务，支持 IPv4 与 IPv6。

> 典型使用场景：在运行 chroot 容器（如 Termux 中的 Ubuntu）的 Android / Linux 设备上部署 sshd，
> 然后用另一台手机远程监控其状态。

## 功能

- 实时状态监控：CPU 使用率 / 核心数、GPU 名称、内存占用、CPU 温度、电池温度、电量百分比、充电状态、系统负载
- 服务管理：以 SSH 方式远程执行启停命令（可自定义），查看服务运行状态与端口
- 服务卡片管理：运行中的服务卡片支持编辑、删除
- 多服务器：自由添加 / 编辑 / 删除服务器，选择是否显示在主页
- 双栈网络：支持 IPv4 与 IPv6（含纯 IPv6 全局地址）
- SSH 密钥登录：使用 OpenSSH 私钥认证，密钥仅保存在本机
- 配置导入导出：以 JSON 文件备份 / 恢复服务器与服务配置，方便迁移与分享

## 界面

底部三个 Tab：

| Tab | 说明 |
| --- | --- |
| 首页 | 显示服务器状态卡片（CPU / 内存 / 温度 / 电池）与服务状态列表 |
| 服务器 | 管理服务器连接信息（地址 / 端口 / 用户名 / SSH 私钥） |
| 关于 | 作者信息、GitHub 仓库入口、版本与检查更新（含「设置」子页：配置导入导出、开发者调试日志导出） |

UI 采用 Material 3 设计风格，深色扁平化卡片布局（参考 FlClash 视觉风格），在 120Hz 高刷屏幕上滑动流畅。

## 使用前提

- 手机系统：Android 8.0 及以上（minSdk 24）
- 被监控的设备需运行 SSH 服务（OpenSSH），并已把客户端公钥加入 `authorized_keys`
- 需要获取系统状态（CPU / 温度 / 电池）时，被监控端需有相应读取权限（如 Android 设备 root + chroot 环境可读取系统文件）

## 使用方法

1. 安装 APK（见 [Releases](https://github.com/qwqZYLqwq/servermonitor/releases)）
2. 进入「服务器」页，点击右上角 + 添加服务器：
   - 名称：任意命名
   - 地址：服务器 IPv4 或 IPv6 地址
   - 端口：SSH 端口（默认 22）
   - 用户名：SSH 登录用户（如 `root`）
   - SSH 私钥：粘贴 OpenSSH 格式私钥（`-----BEGIN OPENSSH PRIVATE KEY-----` 或 PEM 格式）
3. 回到「首页」，下拉刷新即可查看服务器状态与服务列表
4. 在「关于」页可导出 / 导入配置文件

### 服务器侧示例（chroot Ubuntu / Linux）

```bash
# 生成客户端密钥对（在客户端完成）
ssh-keygen -t ed25519 -f ~/.ssh/servermonitor -N ""

# 将公钥加入服务器 authorized_keys（权限须为 700/600）
echo "ssh-ed25519 AAAA... 你的公钥" >> ~/.ssh/authorized_keys

# 确保 sshd 允许公钥登录
grep -E "PubkeyAuthentication|PermitRootLogin" /etc/ssh/sshd_config
```

## 从源码构建

要求：JDK 17+、Android SDK（compileSdk 35）

```bash
export JAVA_HOME=<JDK 路径>
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

项目使用 Jetpack Compose + Material 3 + JSch（SSH）。

## 隐私说明

- SSH 私钥与服务器配置仅存储在本机应用沙盒目录，不会上传
- 应用不收集任何统计信息

## 更新日志

### v1.2.0

- 新增：「关于」页改为「关于 / 设置」双选项卡，配置导入导出归类到设置页
- 新增：开发者调试 —— 一键导出本应用运行日志（最近 3000 行），方便排查问题
- 新增：支持自定义服务器状态更新间隔（每台服务器可独立设置 2~300 秒，默认 10 秒）
- 新增：关于页显示当前版本与 Git 构建代码，支持一键检查 GitHub 最新版本
- 界面：GitHub 头像与昵称同步（awaZYLawa）；状态栏与 UI 背景颜色统一，顶部间距收紧
- 修复：应用内 GitHub 链接与更新检查使用真实仓库地址，避免 404

### v1.1.0

- 性能优化：主页滑动大幅改流畅，120Hz 高刷下几乎不掉帧（局部重组、列表 key 化、状态按值变化刷新、release 构建交付）
- 修复：修改运行中服务名称不生效 / 保存后出现重复卡片的问题（改用稳定 ID 定位）
- 新增：服务管理卡片支持删除（带二次确认）
- 界面：关于页头像改为圆形并加主题色描边；电量卡片在充电时显示「电量 · 已充电」文字提示
- 修复：充电状态识别更准确（适配不同设备的电池节点，支持 AC/USB 在线检测）

### v1.0.0

- 初始版本：实时状态监控、服务启停管理、多服务器、配置导入导出、IPv4/IPv6

## 许可证

[GPL-3.0](LICENSE)

## 相关

- 提交 Bug 或建议：<https://github.com/qwqZYLqwq/servermonitor/issues>
