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
