# DeadRecall

DeadRecall 是一個為 **Minecraft 26.2 / Fabric** 打造的背包模組，提供多等級背包與死亡物品回收機制，讓你在冒險、挖礦與戰鬥時更安心。

## 特色

- **4 種等級背包**：基礎、標準、進階、獄髓
- **容量逐步升級**：從小型收納到更大容量整理
- **關閉即保存**：背包內容會自動保留
- **死亡背包**：玩家死亡時自動收集掉落物
- **防火保護**：重要背包不易被火焰破壞
- **可選 Discord 橋接**：可把聊天與死亡訊息轉發到 Discord
- **火藥合成**：可先用木碗製作硫磺粉末，再搭配硝石與木炭/煤炭合成火藥

## 使用方式

- 右鍵即可開啟背包
- 物品放入後會自動保存
- 死亡後可在死亡地點找到死亡背包，取回掉落物

## 支援環境

- Minecraft **26.2**
- **Fabric Loader**
- **Fabric API**
- **Java 25**

## 備註

如果你有啟用 Discord 橋接，請先確認相關設定檔已正確填寫。

硝石目前可由砂礫、骨粉與木炭合成取得；硫磺則可先用木碗加工成硫磺粉末，再進一步製作火藥。

如果你在 IDE 內要啟動 client，請直接到 **Gradle → fabric → runClient** 執行。  
不要使用舊的 **Application** run config，也不要手動填 `net.fabricmc.devlaunchinjector.Main`。  
你截圖裡的這個 `runClient` 就是正確的啟動方式。

macOS 使用時可以先執行 `./setup-java.sh ./gradlew runClient`，它會自動找 Java 25 並用正確的環境啟動。

如果還是出現 `Minecraft 26.2 requires Java 25 but Gradle is using 21`，請到 IDE 設定把 **Gradle JVM** 改成 **Java 25**。

如果你遇到：
`ClassTweakerFormatException: Namespace (official) does not match current runtime namespace (named)`  
專案已內建 `normalizeFabricApiTweakers` 修正流程，直接執行 `./gradlew runClient` 即可自動處理；  
也可以手動先跑 `./gradlew normalizeFabricApiTweakers` 再啟動。

---

**DeadRecall** 讓你的生存與冒險更輕鬆，也讓物品管理更直覺。

## 自動發布設定

repository 會在 `master` 上的 `gradle.properties` 發生變更時執行 `Publish Modrinth` workflow。流程會先完成 Java 25 Gradle build 與 Server GameTests，再以 `mod_version`、`minecraft_version`、`docs/releases/<version>.md` 和正式 JAR 建立 Modrinth 版本。

啟用前，先在 Modrinth 建立 DeadRecall 專案，然後到 GitHub repository 的 **Settings → Secrets and variables → Actions** 設定：

- Repository variable `MODRINTH_PROJECT_ID`：Modrinth 專案的固定 project ID。不要使用可能改名的 slug。
- Repository secret `MODRINTH_TOKEN`：具有讀取該專案及建立版本權限的 Modrinth personal access token。

Token 不可寫入 repository、issue、PR、workflow input 或 build log。

### 發布規則

- 只有 `master` 上 `gradle.properties` 的變更會自動觸發，不會在 PR 或一般 build 上傳。
- `mod_version` 必須有 `docs/releases/<version>.md`，並列在 `docs/releases/README.md`。
- 穩定版本發布為 `release`；版本號含 `beta` 或 `rc` 時發布為 `beta`，含 `alpha` 時發布為 `alpha`。
- Fabric API 標記為必要依賴，Trinkets Updated 標記為可選依賴。
- 同版本、同 SHA-512 JAR 會安全略過；同版本但 JAR 不同會失敗，不會覆蓋已發布檔案。
- 設定完成後可從 Actions 手動執行 `Publish Modrinth`，補發目前版本或重試失敗的發布。

本機只驗證 metadata 與 JAR、不連線 Modrinth：

```bash
MODRINTH_DRY_RUN=true .github/scripts/publish-modrinth.sh
```
