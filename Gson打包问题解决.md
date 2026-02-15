# 🔧 Gson 仍未打包的解决方案

## ❌ 当前问题

虽然：
- ✅ Java 环境已设置
- ✅ 构建成功完成
- ✅ JAR 文件已生成

但是：
- ❌ **Gson 仍然没有被打包进 JAR**

## 🔍 原因分析

`build.gradle` 中的配置可能需要使用 Fabric Loom 特定的语法。

### 之前的配置（可能不生效）
```groovy
implementation 'com.google.code.gson:gson:2.10.1'
include 'com.google.code.gson:gson:2.10.1'
```

### 修复后的配置
```groovy
implementation(include('com.google.code.gson:gson:2.10.1'))
```

这是 Fabric Loom 推荐的依赖打包方式。

---

## ✅ 解决方案

### 方法 1: 使用修复脚本（推荐）

我已经更新了 `build.gradle`，现在执行：

```powershell
.\quick-rebuild.ps1
```

这个脚本会：
1. ✅ 强制清理所有 Gradle 缓存
2. ✅ 删除 build 目录和 loom-cache
3. ✅ 重新执行 clean 和 build
4. ✅ 自动验证 Gson 是否已打包
5. ✅ 显示明确的成功或失败消息

---

### 方法 2: 在 IntelliJ IDEA 中构建

如果命令行方式仍然有问题，在 IDE 中尝试：

1. **重新加载 Gradle 项目**
   - 右键点击 `build.gradle`
   - 选择「Reload Gradle Project」
   - 或点击 Gradle 面板的刷新按钮 🔄

2. **清理并重建**
   - Build → Clean Project
   - Build → Rebuild Project

3. **检查输出**
   - 在 Build 输出中查找 "including" 或 "gson" 相关的信息

4. **验证 JAR**
   - 构建完成后执行 `.\verify-jar.ps1`

---

## 📊 预期结果

### 成功标志

执行 `.\quick-rebuild.ps1` 后应该看到：

```
✅ 成功！Gson 已打包！
  ✅ 找到 XX 个 Gson 文件
  ✅ JAR 大小: 300-350 KB
```

### JAR 大小对比

| 状态 | 大小 | 说明 |
|------|------|------|
| ❌ 无 Gson | ~260 KB | 当前状态 |
| ✅ 有 Gson | **~300-350 KB** | 目标状态 |

---

## 🔧 替代方案：使用 Shadow Plugin

如果 Fabric Loom 的 `include` 仍然不工作，可以使用 Shadow Plugin：

### 1. 修改 `build.gradle`

在文件顶部添加：
```groovy
plugins {
    id 'fabric-loom' version '1.15-SNAPSHOT'
    id 'maven-publish'
    id 'com.github.johnrengelman.shadow' version '8.1.1'  // 新增
}
```

在 dependencies 部分：
```groovy
dependencies {
    // ... 其他依赖 ...
    
    // Shadow 方式打包 Gson
    shadow 'com.google.code.gson:gson:2.10.1'
}
```

添加 shadowJar 任务：
```groovy
shadowJar {
    configurations = [project.configurations.shadow]
    archiveClassifier.set('all')
}

jar {
    dependsOn shadowJar
}
```

### 2. 构建
```powershell
.\gradlew.bat clean shadowJar
```

---

## 🎯 最简单的方法

实际上，既然 `DiscordBridge.java` 使用了 Gson，而 Minecraft/Fabric 内部已经包含 Gson，**最简单的方法是使用 Minecraft 提供的 Gson**。

### 修改方案：不打包 Gson，使用 Minecraft 的 Gson

在 `build.gradle` 中：
```groovy
dependencies {
    // ... 其他依赖 ...
    
    // 使用 Minecraft 提供的 Gson（已包含在 Minecraft 中）
    compileOnly 'com.google.code.gson:gson:2.10.1'
}
```

**优点**：
- ✅ 不需要打包 Gson
- ✅ JAR 文件更小
- ✅ 不会有依赖冲突
- ✅ 使用 Minecraft 已经加载的 Gson

**缺点**：
- ⚠️ 依赖 Minecraft 提供的 Gson 版本

---

## 📋 推荐流程

### 优先尝试：

1. **执行修复脚本**
   ```powershell
   .\quick-rebuild.ps1
   ```

2. **如果仍然失败，使用 Minecraft 的 Gson**
   - 修改 `build.gradle`：
     ```groovy
     compileOnly 'com.google.code.gson:gson:2.10.1'
     ```
   - 删除 `include` 相关行
   - 重新构建

3. **在 IDE 中构建**
   - Reload Gradle Project
   - Build → Rebuild Project

---

## 🎬 现在执行

```powershell
.\quick-rebuild.ps1
```

这个脚本会自动验证结果并告诉你是否成功！

如果还是失败，我会提供使用 Minecraft 内置 Gson 的方案。
