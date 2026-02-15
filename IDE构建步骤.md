# 🚀 在 IntelliJ IDEA 中重新构建

## 📋 请按照以下步骤操作

### 步骤 1：重新加载 Gradle 项目

1. 在 IntelliJ IDEA 的项目面板（Project）中
2. 找到并**右键点击** `build.gradle` 文件
3. 选择「**Reload Gradle Project**」或「**重新加载 Gradle 专案**」
4. 或者：打开右侧的 **Gradle** 面板，点击 🔄 **刷新**按钮
5. **等待同步完成**（可能需要 30 秒 - 1 分钟）
   - 底部会显示进度条
   - 等待显示「BUILD SUCCESSFUL」

---

### 步骤 2：清理项目

1. 点击菜单栏的 **Build**
2. 选择 **Clean Project**（清理项目）
3. 等待完成

---

### 步骤 3：重建项目

1. 点击菜单栏的 **Build**
2. 选择 **Rebuild Project**（重建项目）
3. **这是最重要的一步！**
4. 等待 1-2 分钟完成
   - 底部的 Build 窗口会显示进度
   - 等待显示「BUILD SUCCESSFUL」

---

### 步骤 4：验证结果

#### 在 IDE 中检查：

1. 展开项目结构：
   ```
   DeadRecall
   └── build
       └── libs
           ├── deadrecall-1.1.0.jar        ← 检查这个
           └── deadrecall-1.1.0-sources.jar
   ```

2. **右键点击** `deadrecall-1.1.0.jar`
3. 选择「Show in Explorer」或「在文件管理器中显示」
4. 查看文件大小

#### 或在 PowerShell 中检查：

```powershell
# 查看 JAR 文件大小
(Get-Item "build\libs\deadrecall-1.1.0.jar").Length / 1KB

# 应该显示：约 260 KB
```

---

## ✅ 成功标志

如果看到：
- ✅ **BUILD SUCCESSFUL** 在 Build 输出中
- ✅ JAR 文件大小：**250-280 KB**（而不是 13 KB）
- ✅ 没有编译错误

**恭喜！构建成功！**

---

## 🎯 构建成功后的下一步

### 部署到 Minecraft：

```powershell
# 复制 JAR 到 Minecraft mods 文件夹
Copy-Item "build\libs\deadrecall-1.1.0.jar" -Destination "$env:APPDATA\.minecraft\mods\" -Force
```

### 测试模组：

1. 启动 Minecraft（使用 Fabric Loader 1.21.1）
2. 检查模组列表中是否有 **DeadRecall 1.1.0**
3. 进入游戏测试：
   - 发送聊天消息（测试 Discord Bridge）
   - 死亡后使用 `/back` 指令
4. 查看日志确认没有错误

---

## ❌ 如果仍然失败

### 检查 Build 输出

在 IDE 底部的 **Build** 窗口中：
- 查看是否有 **红色错误消息**
- 查看是否有编译失败的类

### 常见问题：

1. **Gradle 同步失败**
   - 检查网络连接
   - 重试：File → Invalidate Caches → Restart

2. **Java 版本问题**
   - 确认项目使用 Java 21
   - File → Project Structure → Project SDK

3. **源代码错误**
   - 检查 DiscordBridge.java 是否有语法错误
   - 确认所有导入正确

---

## 📞 需要帮助？

如果构建后 JAR 大小仍然是 13 KB：
1. 截图 Build 输出窗口
2. 告诉我看到了什么错误
3. 我会帮你诊断

---

## 🎬 现在开始

**请在 IntelliJ IDEA 中执行：**

1. ✅ **Reload Gradle Project**（右键 build.gradle）
2. ✅ **Build → Clean Project**
3. ✅ **Build → Rebuild Project**
4. ✅ **检查 JAR 大小**

**祝你成功！** 🚀
