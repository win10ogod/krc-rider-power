# KRC Rider Power（KRC 騎士之力）

[English](README.md) · [下載](https://github.com/win10ogod/krc-rider-power/releases) · [MIT 授權](LICENSE)

一個由伺服器統一控制的通用 Buff。管理員設定一組共用基礎值，行善強化「騎士之力」，作惡累積的業力則啟用「邪惡騎士之力」，以最大生命換取更強傷害；不依年代、騎士名稱或形態建立清單。

普通玩家不能修改數值；管理員可在遊戲內編輯。新增形態只要仍使用 KRC 的 `RiderDriverItem` 與完整變身介面，就不需要為它新增對照資料。

## 安裝

1. 使用 **Minecraft 1.21.1、Java 21、NeoForge 21.1.244 或以上的 21.1 版本**。
2. 安裝 Kamen Rider Craft，以及它要求的 GeckoLib、Player Animation Library。
3. 把 `krc-rider-power-1.2.0.jar` 放進遊戲的 `mods` 資料夾。多人遊戲的伺服器與客戶端都要更新至 1.2.0；新版網路協定不接受舊版客戶端。
4. 穿齊 KRC 裝備並完成變身，會自動出現 **騎士之力** Buff。解除變身、裝備不完整、死亡或進入觀察者模式後，加成會移除。

已實際測試的組合：KRC **1.1.3**、NeoForge **21.1.244**、GeckoLib **4.9.2**、Player Animation Library **1.1.6+mc.1.21.1**。KRC 未來版本若改動共用 Java 介面，仍可能需要更新附加模組。

## 預設基礎加成

| 數值 | 中立善惡值時的共用設定 |
| --- | --- |
| 傷害 | ×1.5 |
| 最大生命 | ×1.5 |
| 護甲 | +4 |
| 護甲韌性 | +2 |
| 移動速度 | ×1.15 |
| 抗擊退 | +0.1 |

傷害倍率在護甲、抗性結算前套用一次，涵蓋傷害來源能追溯到該玩家的近戰、投射物及 KRC 技能；不放大自己的自傷。沒有記錄玩家來源的固定傷害不會被誤判為玩家攻擊。

其他加成疊加於原本屬性之上，保留 KRC 效果、裝備與其他模組的修飾值。Minecraft 本身的屬性上限仍適用，例如護甲 30、護甲韌性 20、抗擊退 1。儲存的設定不會被偷偷改成其他值。

## 行善與作惡

善惡值從 **0** 開始，預設範圍 **−100～+100**。

| 行為 | 善惡值 |
| --- | --- |
| 首次成功救治符合條件的殭屍村民 | +10 |
| 擊殺符合條件的敵對怪物 | +1 |
| 殺害村民或流浪商人 | −20 |
| 殺害動物（包含馴養動物）、玩家 | 0 |

未變身時也會記分，完成變身後才套用騎士之力。每 24,000 個伺服器遊戲 tick 共用 **30 點正分上限**，死亡、登出或切換維度都不會清除紀錄。一般生怪磚、生怪蛋、指令、發射器與召喚來源不給正分；試煉生怪磚的戰鬥仍可加分。村民遭感染後的救治與反覆感染救治不給分，避免製造受害者刷取獎勵。

善惡值為負時顯示為正數「業力」：例如 −100 就是業力 100，啟用**邪惡騎士之力**；0 或正值則使用騎士之力。正向分數顯示為「功德」，兩者沿用同一份存檔，同時只生效一種力量。

| 狀態 | 傷害倍率 | 最大生命倍率 |
| --- | --- | --- |
| 業力 100 | ×2.5 | ×0.5 |
| 業力 50 | ×2 | ×1 |
| 中立 | ×1.5 | ×1.5 |
| 功德 100 | ×2 | ×2 |

邪惡路線提高傷害並降低最大生命，另外四項額外加成仍隨業力增加而減少。最大生命切換保留血量百分比，例如 `12/30 → 4/10 → 16/40`；不會持續扣血，也不能靠切換補滿生命。

玩家使用 `/krcboost karma` 查看分數。管理員可編輯 `config/krcboost-karma.json`，再執行 `/krcboost reload`；普通玩家沒有改分入口。新增 `evilDamageBonusScale`（預設 3）及 `evilHealthMultiplier`（預設 0.5）控制最高業力的傷害與生命；舊設定缺少欄位時沿用新預設，不必刪除設定或存檔。將善惡設定的 `enabled` 設成 `false`，會保留分數紀錄並恢復共用基礎加成。

完整規則、設定檔與防刷分條件見 [善惡系統說明](docs/karma.md)。

## 管理員編輯

- `/krcboost status`：所有玩家皆可查看共用基礎設定、善惡值與自身變身資格。
- `/krcboost karma`：查看善惡值、功德、業力、力量種類、傷害／生命倍率與記分規則。
- `/krcboost edit`：開啟管理員編輯畫面，修改後按「儲存並套用全服」。需要 **權限等級 2**。
- `/krcboost reload`：管理員在磁碟編輯設定後，重新載入基礎加成與善惡兩份設定。也可從伺服器主控台執行。

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

- 指令與網路儲存請求皆由伺服器檢查管理員權限，共用基礎設定與善惡分數皆沒有普通玩家自訂入口。
- 伺服器讀取實際裝備與 KRC 變身狀態；只有腰帶、裝備混搭或尚在變身動畫中，都不會生效。
- 正邪力量同時只保留一種，共用固定修飾值識別碼，重複更新不會堆疊加成。手動加入的高等級 Buff 會被校正為等級 I；未變身時則會移除。
- 最大生命改變時保留血量百分比。例如 `8/20 → 12/30 → 8/20`，反覆變身無法補滿生命。
- 登出時先移除臨時修飾值並還原血量比例，再由 Minecraft 儲存玩家資料；重新登入不會因血量上限切換而增加生命。
- 攻擊與受傷前會再次檢查資格，避免裝備剛移除時沿用舊加成。

以上檢查負責本模組的權限與加成一致性，不能取代伺服器對其他模組或移動封包的管理。

## 建置與驗證

Gradle 會自動從 KRC 作者的 Modrinth 發佈來源下載釘選版本，供編譯及開發測試使用，不需要自行把原始 JAR 放在專案中，也不會包進成品。

```bash
./gradlew test runGameTestServer build
```

Windows 使用 `gradlew.bat test runGameTestServer build`。需要 JDK 21。成品位於 `build/libs/krc-rider-power-1.2.0.jar`；同目錄的 `-sources.jar` 是原始碼，請勿當作遊戲模組安裝。

1.2.0 驗證記錄見 [邪惡騎士之力驗證](evidence/evil-power-verification.md)，1.1.0 記錄見 [善惡系統驗證](evidence/karma-verification.md)，原版記錄見 [1.0.0 驗證](evidence/verification.md)。管理員畫面截圖見 [rider-power-editor.png](evidence/rider-power-editor.png)。測試程式在獨立 `gametest` source set 中，不會進入成品。

此專案不修改或重新封裝 KRC 原始 JAR。

## KRC 自動更新與建置

GitHub Actions 每 6 小時檢查適用於 Minecraft 1.21.1／NeoForge 的 KRC 正式版。發現新版後先執行完整建置、單元測試與 GameTest；通過後更新 `main` 的 KRC 依賴版本，並發佈附有 JAR 的相容性預發佈版本。失敗會保留現有依賴與版本，並提供建置日誌。

也可在 [Actions](https://github.com/win10ogod/krc-rider-power/actions) 的 **Update KRC and build** 頁面按 **Run workflow**。詳細流程與排程限制見 [自動更新說明](docs/automatic-updates.md)。

## MIT 與 CurseForge

本附加模組程式碼與原創素材以 [MIT](LICENSE) 開源；外部依賴各自保留原有授權。這是 Kamen Rider Craft 的非官方附加模組。

[CurseForge 上架資料](publishing/curseforge-project.md) 包含英文介紹、專案欄位、圖示、相依套件與更新紀錄。
