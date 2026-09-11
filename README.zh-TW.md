# KRC Rider Power（KRC 騎士之力）

[English](README.md) · [下載](https://github.com/win10ogod/krc-rider-power/releases) · [MIT 授權](LICENSE)

一個由伺服器統一控制的通用 Buff。所有完成 KRC 變身的玩家共用同一組加成，不依年代、騎士名稱或形態建立清單。

普通玩家不能修改數值；管理員可在遊戲內編輯。新增形態只要仍使用 KRC 的 `RiderDriverItem` 與完整變身介面，就不需要為它新增對照資料。

## 安裝

1. 使用 **Minecraft 1.21.1、Java 21、NeoForge 21.1.244 或以上的 21.1 版本**。
2. 安裝 Kamen Rider Craft，以及它要求的 GeckoLib、Player Animation Library。
3. 把 `krc-rider-power-1.0.0.jar` 放進遊戲的 `mods` 資料夾。多人遊戲的伺服器與客戶端都要安裝。
4. 穿齊 KRC 裝備並完成變身，會自動出現 **騎士之力** Buff。解除變身、裝備不完整、死亡或進入觀察者模式後，加成會移除。

已實際測試的組合：KRC **1.1.3**、NeoForge **21.1.244**、GeckoLib **4.9.2**、Player Animation Library **1.1.6+mc.1.21.1**。KRC 未來版本若改動共用 Java 介面，仍可能需要更新附加模組。

## 預設加成

| 數值 | 共用設定 |
| --- | --- |
| 傷害 | ×1.5 |
| 最大生命 | ×1.5 |
| 護甲 | +4 |
| 護甲韌性 | +2 |
| 移動速度 | ×1.15 |
| 抗擊退 | +0.1 |

傷害倍率在護甲、抗性結算前套用一次，涵蓋傷害來源能追溯到該玩家的近戰、投射物及 KRC 技能；不放大自己的自傷。沒有記錄玩家來源的固定傷害不會被誤判為玩家攻擊。

其他加成疊加於原本屬性之上，保留 KRC 效果、裝備與其他模組的修飾值。Minecraft 本身的屬性上限仍適用，例如護甲 30、護甲韌性 20、抗擊退 1。儲存的設定不會被偷偷改成其他值。

## 管理員編輯

- `/krcboost status`：所有玩家皆可查看共用設定與自身變身資格。
- `/krcboost edit`：開啟管理員編輯畫面，修改後按「儲存並套用全服」。需要 **權限等級 2**。
- `/krcboost reload`：管理員在磁碟編輯設定後重新載入。也可從伺服器主控台執行。

設定檔首次啟動時產生於 `config/krcboost.json`。也可以參考 [預設設定](src/main/resources/krcboost/default-config.json)。

```json
{
  "enabled": true,
  "attackMultiplier": 1.5,
  "healthMultiplier": 1.5,
  "armorBonus": 4.0,
  "toughnessBonus": 2.0,
  "speedMultiplier": 1.15,
  "knockbackBonus": 0.1
}
```

倍率必須至少為 1，加值至少為 0，抗擊退加值介於 0～1；所有數字必須有限。`enabled: false` 會停用並移除通用 Buff。無效檔案不會被預設值覆寫，重新載入失敗時繼續使用上一份有效設定。

「載入預設」只改變編輯畫面，按儲存才生效；關閉畫面不會儲存。若另一位管理員已更新設定，舊畫面的儲存請求會被拒絕，避免覆寫較新的設定。

## 防止自行改值、疊加與刷血

- 指令與網路儲存請求皆由伺服器檢查管理員權限，沒有個人加成或普通玩家自訂入口。
- 伺服器讀取實際裝備與 KRC 變身狀態；只有腰帶、裝備混搭或尚在變身動畫中，都不會生效。
- 一種 Buff、一組固定修飾值識別碼，重複更新不會堆疊加成。手動加入的高等級 Buff 會被校正為等級 I；未變身時則會移除。
- 最大生命改變時保留血量百分比。例如 `8/20 → 12/30 → 8/20`，反覆變身無法補滿生命。
- 登出時先移除臨時修飾值並還原血量比例，再由 Minecraft 儲存玩家資料；重新登入不會因血量上限切換而增加生命。
- 攻擊與受傷前會再次檢查資格，避免裝備剛移除時沿用舊加成。

以上檢查負責本模組的權限與加成一致性，不能取代伺服器對其他模組或移動封包的管理。

## 建置與驗證

Gradle 會自動從 KRC 作者的 Modrinth 發佈來源下載釘選版本，供編譯及開發測試使用，不需要自行把原始 JAR 放在專案中，也不會包進成品。

```bash
./gradlew test runGameTestServer build
```

Windows 使用 `gradlew.bat test runGameTestServer build`。需要 JDK 21。成品位於 `build/libs/krc-rider-power-1.0.0.jar`；同目錄的 `-sources.jar` 是原始碼，請勿當作遊戲模組安裝。

驗證記錄見 [evidence/verification.md](evidence/verification.md)。管理員畫面截圖見 [rider-power-editor.png](evidence/rider-power-editor.png)。測試程式在獨立 `gametest` source set 中，不會進入成品。

此專案不修改或重新封裝 KRC 原始 JAR。

## KRC 自動更新與建置

GitHub Actions 每 6 小時檢查適用於 Minecraft 1.21.1／NeoForge 的 KRC 正式版。發現新版後先執行完整建置、單元測試與 GameTest；通過後更新 `main` 的 KRC 依賴版本，並發佈附有 JAR 的相容性預發佈版本。失敗會保留現有依賴與版本，並提供建置日誌。

也可在 [Actions](https://github.com/win10ogod/krc-rider-power/actions) 的 **Update KRC and build** 頁面按 **Run workflow**。詳細流程與排程限制見 [自動更新說明](docs/automatic-updates.md)。

## MIT 與 CurseForge

本附加模組程式碼與原創素材以 [MIT](LICENSE) 開源；外部依賴各自保留原有授權。這是 Kamen Rider Craft 的非官方附加模組。

[CurseForge 上架資料](publishing/curseforge-project.md) 包含英文介紹、專案欄位、圖示、相依套件與更新紀錄。
