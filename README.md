國考學習 App

一款專為高普考 / 地方特考考生打造的 Android 學習工具，整合 AI 出題、AI 申論題評分、單字發音與模擬考排名，協助使用者更有效率地準備英文科與申論題科目。



專案動機


英文單字練習缺乏即時、客製化的出題與講解
申論題沒有即時批改管道，難以知道自己寫得好不好、缺什麼


這個 App 使用 Gemini AI 來補齊這兩塊：自動出英文選擇題、AI 講解單字、AI 批改申論題並給出結構化評分與弱點分析，省去考生需要頻繁開啟網頁版並且無法快速找到自己做過題目的區塊issue。

核心功能

📖 英文練習模組


單字庫管理：分四大詞性（名詞/動詞/副詞/形容詞），支援增刪查、Trie 結構即時前綴搜尋
AI 出題：呼叫 Gemini API 依詞性比例、弱點搶救模式動態生成選擇題
錯題追蹤：自動記錄答錯單字與錯誤次數，生成個人化錯題本
AI 單字講解：KK 音標、詞性、例句一次生成，並串接 Google TTS 下載發音快取

(結合網路程式設計課程測試用)測驗歷史：完整紀錄每次練習/模擬考成績，支援逐題回顧
(結合網路程式設計課程測試用)模擬考排名：透過 TCP Socket 連線伺服器，即時同步考生排名，支援離線快取


✏️ 申論題模組


歷屆考古題庫：依考試類別（高考/普考/地方特考）、年份、科目分類管理，支援批次匯入與版本控管避免重複
AI 評分流程：四步驟（複製題目 Prompt → 貼上作答 → 取得 AI 評分 Prompt → 貼回結果）取得結構化評分，包含總分、總評、弱點分析
作答歷史：可回顧每次申論題作答內容與 AI 評分結果，並與來源考古題關聯查詢



技術架構


平台：Android（Java）
AI 整合：Gemini API（非同步出題、單字講解、申論題評分）
語音：Google TTS API，本地音訊快取管理
(結合網路程式設計課程測試用)網路：TCP Socket 用於模擬考即時排名推播
資料儲存：本地檔案（txt）+ SharedPreferences，使用本地儲存，無須連線。
資料結構：實作 Trie 用於單字庫前綴搜尋與即時篩選
架構分層：

core/：共用服務層（狀態管理、API 串接、檔案 I/O、資料模型）
english/：英文練習模組（出題、作答、歷史、排名）
essay/：申論題模組（考古題管理、AI 評分、作答紀錄）





模組導覽結構

LauncherActivity
├── english/ → MainActivity（英文練習）
│     ├── SetupActivity → QuizActivity → ResultActivity → QuizReviewActivity
│     ├── HistoryActivity → QuizReviewActivity / RankingActivity
│     ├── WordBankActivity → WordExplainActivity
│     └── OnlineExamActivity → RankingActivity
└── essay/ → EssayMainActivity（申論題）
      ├── EssayEditActivity → EssayDetailActivity
      └── ExamCategoryActivity
            └── PastExamYearActivity
                  └── PastExamActivity
                        └── QuestionDetailActivity
                              ├── EssayEditActivity（從考古題作答）
                              └── EssayDetailActivity（查看紀錄）


設計重點


狀態管理：以 AppState 作為全域單例，集中管理單字庫、Trie 索引、目前測驗、API Key、模擬考 session 等跨頁面共享狀態，避免 Activity 間傳遞冗餘資料
資料解耦：AI 回傳的非結構化文字（出題結果、評分結果）統一透過 Parser 類別（QuizParser、EssayResultParser）轉換為結構化物件，降低 UI 層與 AI 輸出格式的耦合
可擴充性：考古題匯入採版本控管（imported_version），新增考試類別或欄位只需修改對應的 Manager / Importer，不影響既有資料



安裝與執行

bashgit clone https://github.com/94666xdxd87-cyber/-app.git
cd -app
./gradlew assembleDebug


需自行於 App 內設定頁輸入 Gemini API Key 才能使用 AI 出題/評分功能。


尚未解決問題:
申論題圖片部分無法正確解析



未來規劃


 新增上傳圖片解析功能，使考生亦可練習申論題手寫。
 優化UI設計介面
 學習自動化測試加速debug
 完善細節功能並套用登入驗證連接帳號後上架

