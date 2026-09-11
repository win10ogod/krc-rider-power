# CurseForge 上架資料

這份資料供建立 **KRC Rider Power** 專案時填寫；尚未提交 CurseForge，也尚未取得 CurseForge 專案 ID。

## 專案欄位

| 欄位 | 填入內容 |
| --- | --- |
| Game | Minecraft |
| Name | KRC Rider Power |
| 建議 Slug | krc-rider-power（以網站實際可用名稱為準） |
| Summary | One configurable, server-controlled buff for Kamen Rider Craft transformations, with shared bonuses and administrator-only editing. |
| Class | Mods |
| Main category | Addons |
| License | MIT License |
| Description | 貼入 [curseforge-description.md](curseforge-description.md) 的英文內容，選擇 Markdown 編輯模式 |
| Logo | [icon.png](icon.png)，512 × 512 PNG；可編輯原稿為 [icon.svg](icon.svg) |
| Source | https://github.com/win10ogod/krc-rider-power |
| Issues | https://github.com/win10ogod/krc-rider-power/issues |
| Screenshot | [rider-power-editor.png](../evidence/rider-power-editor.png) |
| Screenshot title | Rider Power administrator editor |
| Screenshot caption | Configure the six shared bonuses. The current editor uses Traditional Chinese. Saving requires server operator permission level 2. |

## 第一個檔案

| 欄位 | 填入內容 |
| --- | --- |
| Upload file | `krc-rider-power-1.0.0.jar`，位於本地 `dist/1.0.0/` 或 GitHub Release |
| Display name | KRC Rider Power 1.0.0 — NeoForge 1.21.1 |
| Release type | Release |
| Game version | 1.21.1 |
| Mod loader | NeoForge |
| Java version | Java 21（若表單提供此欄位） |
| Changelog | 使用 [CHANGELOG.md](../CHANGELOG.md) 的 1.0.0 段落 |

在專案或檔案的 Relations 中，將下列項目設為 **Required Dependency**：

| 專案 | CurseForge 頁面 | 實測版本 |
| --- | --- | --- |
| Kamen Rider Craft | [kamen-rider-craft](https://www.curseforge.com/minecraft/mc-mods/kamen-rider-craft) | 1.1.3 |
| GeckoLib | [geckolib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.9.2，NeoForge 1.21.1 |
| Player Animation Library | [playeranimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) | 1.1.6+mc.1.21.1，NeoForge |

整包 `curseforge-kit.zip` 是提供給作者的上架材料。實際 Minecraft 模組檔案選其中的成品 JAR；`-sources.jar` 是原始碼附件，不是供玩家安裝的版本。

## 提交流程

1. 開啟 [CurseForge 建立專案](https://authors.curseforge.com/#/projects/create/choose-game)，登入作者帳號並填入上方資料。
2. 貼上英文介紹，選 MIT，使用附帶的原創圖示與遊戲內截圖。
3. 上傳成品 JAR，選 Minecraft 1.21.1、NeoForge、Release，填寫依賴與更新紀錄。
4. 提交後查看網站審核狀態；此本地資料包不代表已通過平台審核。

欄位與提交流程依 [CurseForge 建立專案說明](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project) 整理。圖示尺寸依 [官方上架指南](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips) 準備。檔案類型、依賴與載入器標籤參考 [官方檔案說明](https://support.curseforge.com/support/solutions/articles/9000197242-file-project-types-and-additional-fields)。資料核對日期：2026-09-11。
