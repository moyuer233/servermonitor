# 服务器监控 (ServerMonitor)

一款 Android 服务器监控应用：通过 SSH 连接远程服务器，实时查看 CPU / GPU / 内存 / 温度 / 电池等运行状态，并可直接远程启动、停止、重启服务，支持 IPv4 与 IPv6。

> 典型使用场景：在运行 chroot 容器（如 Termux 中的 Ubuntu）的 Android / Linux 设备上部署 sshd，然后用另一台手机远程监控其状态。

## 功能特性

- **实时状态监控**：CPU 使用率 / 核心数、GPU 名称、内存占用、CPU 温度、电池温度、电量百分比、充电状态、系统负载
- **服务管理**：以 SSH 方式远程执行启停命令（自定义），可查看服务运行状态与端口
- **多服务器**：自由添加 / 编辑 / 删除服务器，选择是否显示在主页
- **双栈网络**：支持 IPv4 与 IPv6（含纯 IPv6 全局地址）
- **SSH 密钥登录**：使用 OpenSSH 私钥认证，密钥仅保存在本机
- **配置导入导出**：以 JSON 文件形式备份 / 恢复服务器与服务配置，方便迁移与分享

## 界面

底部三个 Tab：

| Tab    | 说明                                                       |
| ------ | ---------------------------------------------------------- |
| 首页   | 显示服务器状态卡片（CPU / 内存 / 温度 / 电池）与服务状态列表 |
| 服务器 | 管理服务器连接信息（地址 / 端口 / 用户名 / SSH 私钥）       |
| 关于   | 作者信息、GitHub 仓库入口、提交 Issue、配置导入导出         |

UI 采用 Material 3 设计风格。

## 使用前提

- 手机系统：Android 8.0 及以上（minSdk 24）
- 被监控的设备需运行 SSH 服务（OpenSSH），并已把客户端公钥加入 `authorized_keys`
- 需要获取系统状态（CPU / 温度 / 电池）时，被监控端需有相应读取权限（如 Android 设备 root + chroot 环境可读取系统文件）

## 使用方法

1. 安装 APK（见 [Releases](https://github.com/qwqZYLqwq/servermonitor/releases)）
2. 进入「服务器」页，点击右上角 **+** 添加服务器：
   - **名称**：任意命名
   - **地址**：服务器 IPv4 或 IPv6 地址
   - **端口**：SSH 端口（默认 22）
   - **用户名**：SSH 登录用户（如 `root`）
   - **SSH 私钥**：粘贴 OpenSSH 格式私钥（`-----BEGIN OPENSSH PRIVATE KEY-----` 或 PEM 格式）
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

## 许可证

[GPL-3.0](LICENSE)

## 相关

- 提交 Bug 或建议：<https://github.com/qwqZYLqwq/servermonitor/issues>