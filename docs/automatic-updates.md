# KRC 自動更新與建置

工作流程：[Update KRC and build](https://github.com/win10ogod/krc-rider-power/actions/workflows/update-krc.yml)。

## 觸發與結果

- 排程：每 6 小時一次，UTC 00:23、06:23、12:23、18:23；台灣時間為 08:23、14:23、20:23、02:23。
- 手動：Actions → Update KRC and build → Run workflow。勾選 `force_build` 可在 KRC 未更新時重跑全部測試與建置。
- 偵測來源：KRC 作者在 [Modrinth](https://modrinth.com/mod/kLNLtzWr) 的正式版，限定 Minecraft 1.21.1 與 NeoForge。按發佈時間選新版，不用字串比較版本號，也不自動降版。
- 通過：更新 `gradle.properties` 的 KRC 版本與固定版本 ID，提交到 `main`，建立有模組 JAR、原始碼 JAR、SHA-256 的 GitHub 相容性預發佈版本。
- 失敗：不提交未通過測試的候選依賴，不發佈失敗成品；Actions 保留日誌，原有正式版仍可下載。

偵測以 Modrinth 正式上架時間為準。只在 CurseForge 發佈但尚未同步至 Modrinth 的更新，需要等待作者同步。

## 建置驗證

每個候選版本都執行 Python 更新流程測試，以及：

```bash
./gradlew --no-daemon -Pmod_version=<相容性版本> test runGameTestServer build
```

GameTest 使用候選版本的實際 KRC 驅動器註冊表，檢查完整變身、權限、傷害倍率、重複套用、血量比例、登出與停用行為，也會驗證善惡記分、動物中立、分數保存與防刷分條件。若 KRC 改動 Java 介面、裝備或相依需求導致編譯或測試失敗，工作流程會失敗並保留診斷資料，等待程式碼相容修正。

這個流程追蹤現有 Minecraft 1.21.1 分支。移植到其他 Minecraft／載入器版本仍需相應開發與驗證。

## 版本與重試

例如以 KRC 版本 ID `I61dxvsY` 建置時，產物版本為 `1.1.0+krc.I61dxvsY`，標籤為 `compat-v1.1.0-krc-I61dxvsY`。正式版 `v1.1.0` 保持獨立。自動相容性版本標為 prerelease，並在說明中列出實際測試的 KRC 版本。

即使依賴已更新，若對應的相容性 release 尚未完成，下一次執行仍會建置與補發。檔案先上傳到 draft，全部成功才公開。已公開的相容性版本不會被覆寫；強制重建的產物仍可從 Actions artifacts 下載。

建置期間若有人更新 `main`，自動提交會停止並要求重跑，不會強制推送或合併未經測試的內容。同時只執行一個 KRC 更新工作。

## GitHub 設定與排程

使用儲存庫內建的 `GITHUB_TOKEN`，工作流程宣告 `contents: write` 以提交版本與建立 release；不需另外保存個人存取權杖。排程只在原始 `win10ogod/krc-rider-power` 儲存庫執行，fork 如需啟用，可調整 workflow 的 repository 條件。若日後加上禁止直接推送的分支規則，需把自動更新改為符合該規則的 PR 流程。

GitHub 排程可能延遲，而且公開儲存庫連續 60 天沒有活動時，排程可能自動停用；可在 Actions 重新啟用並手動執行。這是 [GitHub schedule 的平台行為](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#schedule)，每 6 小時是設定頻率，不是準時更新的保證。

自動提交所用的 `GITHUB_TOKEN` 不會再觸發一般 push 工作流程，因此所有測試與發佈都在這個更新工作流程內完成。參考 [GitHub 工作流程觸發說明](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow)。
