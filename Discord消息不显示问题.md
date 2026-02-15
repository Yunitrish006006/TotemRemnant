# 🔍 Discord Bridge 消息不显示问题诊断

## 📊 当前状态

根据日志分析：

### ✅ 正常的部分
- ✅ 模组已加载：`Loading 54 mods: - deadrecall 1.1.0`
- ✅ 代码正常运行：没有报错
- ✅ 聊天功能正常：`[Not Secure] <{USERNAME}> test`

### ❌ 问题所在
- ❌ Discord Bridge 被停用：`[DeadRecall] [DiscordBridge] 功能已停用`

---

## 🎯 问题根源

日志显示 **`[DiscordBridge] 功能已停用`**，这意味着：

### 可能原因 1：配置文件中 `enabled: false`
配置文件：`discord-bridge.json` 中设置为停用

### 可能原因 2：配置文件不存在
模组首次运行时会创建默认配置（默认是 `enabled: false`）

### 可能原因 3：配置文件格式错误
JSON 格式不正确，导致读取失败

---

## 🛠️ 解决步骤

### 步骤 1：找到配置文件位置

根据你使用的启动器，配置文件可能在：

#### 如果使用 ModrinthApp：
```
%APPDATA%\ModrinthApp\profiles\<你的配置文件名>\config\discord-bridge.json
```

#### 如果使用标准启动器：
```
%APPDATA%\.minecraft\config\discord-bridge.json
```

#### 如果使用其他启动器：
查看启动器的游戏目录设置，找到 `config` 文件夹

---

### 步骤 2：检查配置文件

#### 方法 A：使用 PowerShell 搜索

```powershell
# 搜索所有可能的位置
Get-ChildItem "$env:APPDATA" -Recurse -Filter "discord-bridge.json" -ErrorAction SilentlyContinue | Select-Object FullName
```

#### 方法 B：手动查找

1. 按 `Win + R`
2. 输入：`%APPDATA%`
3. 查找包含 Minecraft 的文件夹
4. 进入 `config` 文件夹
5. 找到 `discord-bridge.json`

---

### 步骤 3：修复配置文件

编辑 `discord-bridge.json`，内容应该是：

```json
{
  "enabled": true,
  "workerUrl": "https://mc-discord-bot.yunitrish0419.workers.dev",
  "apiKey": "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"
}
```

**关键点**：
- `"enabled": true` ← 必须是 `true`
- `workerUrl` 和 `apiKey` 必须填写

---

### 步骤 4：重启 Minecraft

关闭游戏，重新启动。

---

### 步骤 5：验证

重启后，查看日志应该看到：

```
[DeadRecall] [DiscordBridge] 已启用，Worker URL: https://mc-discord-bot.yunitrish0419.workers.dev
```

**而不是**：
```
[DeadRecall] [DiscordBridge] 功能已停用  ← 这是问题
```

---

## 🔧 快速修复命令

如果你知道配置文件位置，可以用这个命令快速修复：

```powershell
# 替换 <路径> 为实际的配置文件路径
$configPath = "<路径>\discord-bridge.json"

# 创建正确的配置
@"
{
  "enabled": true,
  "workerUrl": "https://mc-discord-bot.yunitrish0419.workers.dev",
  "apiKey": "mc_ak_7Xp9Qm3vKsW2nF8jRtYb6LdA4eHcZu"
}
"@ | Set-Content $configPath -Encoding UTF8

Write-Host "✅ 配置已更新"
```

---

## 🎯 完整诊断流程

### 1. 找到配置文件

```powershell
# 运行检查脚本
.\check-discord-config.ps1
```

### 2. 确认配置正确

- ✅ `enabled: true`
- ✅ `workerUrl` 已填写
- ✅ `apiKey` 已填写

### 3. 重启 Minecraft

### 4. 检查新日志

应该看到：
```
[DeadRecall] [DiscordBridge] 已启用，Worker URL: https://...
```

### 5. 测试发送消息

在游戏中发送聊天消息，检查：

#### Minecraft 日志：
```
[DiscordBridge] 发送请求到: https://...
[DiscordBridge] API Key: mc_ak_7Xp9...
[DiscordBridge] JSON 内容: {"username":"玩家名","message":"消息"}
[DiscordBridge] 发送成功 (HTTP 200): {"success":true,"data":{"sent":1,"failed":0}}
```

#### Discord 频道：
应该收到消息！

---

## 🚨 如果还是不行

### 检查 Worker 端

1. **Worker 是否正常运行**
   ```powershell
   .\test-discord-api.ps1
   ```

2. **Discord Webhook 是否有效**
   ```powershell
   .\test-webhook.ps1
   ```

3. **查看之前的诊断文档**
   - `快速解决方案.md`
   - `Discord_BRIDGE_诊断报告.md`

---

## 📋 检查清单

- [ ] ✅ 找到 `discord-bridge.json` 配置文件
- [ ] ✅ 配置文件中 `"enabled": true`
- [ ] ✅ `workerUrl` 和 `apiKey` 已填写
- [ ] ✅ 重启 Minecraft
- [ ] ✅ 日志显示 "已启用"（不是"已停用"）
- [ ] ✅ 发送消息测试
- [ ] ✅ 检查 Discord 频道

---

## 🎯 总结

**问题是**：Discord Bridge 功能被停用了

**原因**：配置文件中 `enabled: false` 或配置不存在

**解决**：
1. 找到配置文件
2. 设置 `"enabled": true`
3. 确保 `workerUrl` 和 `apiKey` 正确
4. 重启 Minecraft

**验证**：日志应该显示 "已启用" 而不是 "已停用"

---

## 🚀 立即执行

```powershell
# 1. 搜索配置文件
Get-ChildItem "$env:APPDATA" -Recurse -Filter "discord-bridge.json" -ErrorAction SilentlyContinue | Select-Object FullName

# 2. 或运行自动检查脚本
.\check-discord-config.ps1
```

找到并修复配置后，重启 Minecraft，问题应该就解决了！🎉
