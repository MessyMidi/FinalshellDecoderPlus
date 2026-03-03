<p align="center">
  <img src="./FSDP_repo_logo.png" alt="Logo" />
</p>

<p align="center">
  FinalShell 批量解析工具，用于解析本地 FinalShell 配置目录中的连接配置文件，提取服务器信息，并解密保存的密码或私钥数据。
</p>

> **📌 免责声明**  
> 本项目仅供学习交流使用，使用者需自行承担使用风险。
> 本项目不隶属于 FinalShell 官方，仅用于对本地配置文件进行解析与展示，请勿向 FinalShell 官方反馈任何问题，请勿在 FinalShell 官方论坛等（如有）提及此工具。

---
[![Latest Release](https://img.shields.io/github/v/release/MessyMidi/FinalshellDecoderPlus?display_name=tag)](https://github.com/MessyMidi/FinalshellDecoderPlus/releases/latest)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](./LICENSE)

## ✨ 功能特性

* 匹配默认 FinalShell 安装路径✅
* 批量解析服务器登录信息✅
* 解密加密的登录凭据：
  * 保存的账号密码✅
  * 私钥数据✅
* 表格展示：
  * 连接名称✅
  * 主机✅
  * 端口✅
  * 用户名✅
  * 认证方式✅
  * 登录凭据✅
* 单击字段即可复制凭据等详细信息
* 底部详细信息面板显示完整解析数据

---

## 🖥 快速开始（Windows）

解压release pack后，进入目录：

```
FSDP\
 ├─ FSDP.exe
 ├─ app\
 └─ runtime\
```

双击 `FSDP.exe` 即可运行。

程序会自动尝试检测：

```
C:\Users\<User>\AppData\Local\finalshell
```

如果未自动识别，请手动选择 FinalShell 安装目录（需包含 `conn` 文件夹）。

---

## ⚠ 安全提示

* 本工具仅可用于合法场景（例如数据备份、安全审计、自用环境迁移）。
* 请勿在未授权的环境中使用。
* 请妥善保管导出的敏感信息。

---

## 🛠 自行构建

```powershell
javac -encoding UTF-8 -d out src\FinalShellDecodePass.java
jar --create --file FinalShellDecodePass.jar --main-class FinalShellDecodePass -C out .

jpackage --type app-image ^
  --name FSDP ^
  --input pkg ^
  --main-jar FinalShellDecodePass.jar ^
  --dest dist
```



