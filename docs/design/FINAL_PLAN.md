# হিসাব / Hishab — চূড়ান্ত ইমপ্লিমেন্টেশন পরিকল্পনা

**স্ট্যাটাস:** অনুমোদনের অপেক্ষায়। এই নথি লেখা পর্যন্ত কোনো অ্যাপ কোড পরিবর্তন, commit, push বা release করা হয়নি।  
**ভিত্তি:** ২৪টি ব্যবহারকারী-সিদ্ধান্ত + `docs/review/CODE_AUDIT.md` + `docs/design/DESIGN_SPEC.md` + ১২-স্ক্রিন মকআপ।

---

## ১. লক্ষ্য

পুরনো Kotlin/Compose নেটিভ ভিত্তি রেখে **Hishab / হিসাব** নামে একটি পরিষ্কার, offline-first, বাংলা-প্রথম Android অ্যাপ তৈরি করা। Next.js/WebView সংস্করণের প্রয়োজনীয় ফিচার নেটিভে আনা হবে; raw WebView release রাখা হবে না। সব যাচাইকৃত বাগ ঠিক করা হবে, তারপর টেস্ট/লিন্ট/বিল্ড পাস করলে signed APK release, অবশেষে dead-file cleanup ও নতুন README।

## ২. লক করা সিদ্ধান্ত (সংক্ষিপ্ত)

নাম Hishab/হিসাব; appId `com.hishab.app`; minSdk 29; Kotlin+Compose; drawer primary; paper+emerald; বাংলা default + English digits/theme toggles; expandable bottom-sheet entry; dual accounting; পূর্ণ wallet/due/budget/reminder/undo/JSON+CSV; AI preview+confirm every mutation + আলাদা cloud consent; local-first, Firebase পরে যাচাই; GitHub Releases signed APK+checksum; নতুন key শুধু GitHub Secrets; analytics/ads SDK নেই; legacy migration নেই।

## ৩. ফিচার মার্জ ম্যাট্রিক্স

| ফিচার | উৎস | সিদ্ধান্ত |
|---|---|---|
| আয়/খরচ CRUD, search/filter, date grouping, quantity×unit price | নেটিভ + লেগ্যাসি | রাখা + বাগফিক্স |
| ক্যাটাগরি CRUD, color/icon, category detail | উভয় | রাখা + orphan-safe delete/archive |
| দেনা/পাওনা, partial payment, history | নেটিভ + ব্যবহারকারী | পূর্ণাঙ্গ করা |
| Wallet CRUD/opening balance/transfer/archive | লেগ্যাসি/রোডম্যাপ + ব্যবহারকারী | নতুন নেটিভ বাস্তবায়ন |
| কেনার তালিকা → expense | উভয় | idempotent conversion সহ রাখা |
| Monthly/category budget + 80/100 in-app alert | রোডম্যাপ + ব্যবহারকারী | যোগ; block নয় |
| Reports/charts/CSV | উভয় | exact math + UTF-8 CSV |
| Full JSON backup/restore preview/merge/replace | ব্যবহারকারী | সংস্করণযুক্ত স্কিমা |
| Local due reminders + Calendar shortcut | ব্যবহারকারী | WorkManager/AlarmManager policy + SAF calendar intent |
| AI assistant | উভয় | draft-only, confirm-before-write, offline parser default |
| Google optional / Firebase sync | নেটিভ | v1-এ local; console যাচাইয়ের পরে সক্রিয় |
| Light/dark/system, Bengali/English digits | ব্যবহারকারী | Settings + DataStore |
| Undo/confirm destructive actions | ব্যবহারকারী | সর্বত্র |
| Swipe edit/delete + undo | লেগ্যাসি | **অনুমোদন সাপেক্ষে** অন্তর্ভুক্ত করার প্রস্তাব |
| 60+ icon picker | রোডম্যাপ | **অনুমোদন সাপেক্ষে** প্রসারিত picker |
| Item-wise aggregation insight | রোডম্যাপ | **অনুমোদন সাপেক্ষে** category detail-এ মৌলিক grouping |
| PIN/biometric app lock | রোডম্যাপ | **অনুমোদন সাপেক্ষে**; ডিফল্টে defer প্রস্তাব |
| Google Drive appDataFolder backup | লেগ্যাসি/রোডম্যাপ | **অনুমোদন সাপেক্ষে**; ডিফল্টে defer প্রস্তাব |
| PDF export | রোডম্যাপ | ব্যবহারকারী v1-এ বাদ দিয়েছেন |
| Auto sample seed | নেটিভ/লেগ্যাসি | বাদ; ঐচ্ছিক demo mode আলাদা অনুমোদন ছাড়া নয় |

## ৪. আর্কিটেকচার

- Single Activity + Jetpack Compose Material 3; type-safe Navigation Compose; drawer primary।
- MVVM + UDF: `Screen → ViewModel(StateFlow) → Repository → Room/DAO`; `collectAsStateWithLifecycle`। [2](https://developer.android.com/topic/architecture/recommendations)
- Domain layer: `Money(poisha: Long)`, `AccountingCalculator`, `BudgetCalculator`, `DueCalculator`, `BackupValidator`, `CsvExporter`, `ReminderScheduler`, `AiActionDraft`।
- Room local source of truth; DataStore settings; WorkManager reminder/reschedule; sync outbox ভবিষ্যৎ Firebase-এর জন্য প্রস্তুত কিন্তু v1-এ নিষ্ক্রিয়। [4](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)
- DI: Hilt অথবা হালকা manual DI—চূড়ান্ত সংস্করণ Gradle sync/KSP compatibility যাচাই করে লক করা হবে।
- Firebase/Auth/AI optional adapter; config না থাকলে release build/startup ব্যর্থ হবে না; fake sync-success নিষিদ্ধ।
- Package rename `com.example` → `com.hishab.app`; namespace/applicationId একই।

## ৫. ডেটা মডেল ও স্কিমা (Room v1)

- `Wallet`: id, name, type, openingBalancePoisha, color/icon, archived, timestamps।
- `Category`: id, name, kind(income/expense), color/icon, sortOrder, archived, timestamps।
- `Transaction`: id, kind(income/expense), title, amountPoisha, categoryId, walletId, date, paymentStatus(paid/unpaid/partial), paidAmountPoisha, dueDirection(payable/receivable/null), dueDate, shopOrPerson, quantity/unit/unitPricePoisha, note, sourceEntryId, timestamps, deletedAt।
- `DuePayment`: id, transactionId, walletId, amountPoisha, date, note, createdAt; remaining = amount − Σpayments।
- `Transfer`: id, fromWalletId, toWalletId, amountPoisha, date, note; আয়/খরচ নয়।
- `Todo`: id, title, quantity/unit/estimatedPoisha, targetCategoryId, priority, dueDate, done, convertedTransactionId।
- `Budget`: monthKey, overallLimitPoisha; `CategoryBudget`: monthKey, categoryId, limitPoisha।
- Settings DataStore: theme, digits, reminders, cloudAiConsent, syncEnabled, lastBackupAt।
- Backup JSON schema v1: meta + wallets + categories + transactions + duePayments + transfers + todos + budgets + settings; validation-first, preview, merge/replace, chunked write, round-trip test।
- সব টাকা `Long` poisha; input parser বাংলা/ইংরেজি অঙ্ক, দশমিক, হাজার separator সহ; rounding policy স্পষ্ট।

## ৬. বাগফিক্স কর্মপরিকল্পনা

অডিটের P0 আগে: manifest permission, wrapper/signing, auto-seed, reset/import/backup completeness, dual accounting, AI auto-mutation। তারপর P1: money/date, error propagation, todo conversion, category orphan, dark contrast, touch target, keyboard/sheet, lifecycle state, reminder intent, backup file picker, Firestore rules validation। শেষে P2: R8, unused deps, logs, icon/font weights, report math, markdown rendering, placeholder tests।

## ৭. AI নিরাপত্তা চুক্তি

1. Offline conservative parser default; cloud AI শুধু আলাদা consent-এ।
2. LLM/parser আউটপুট শুধু typed `ActionDraft`; কোনো direct DAO/repository write নয়।
3. Draft-এ before→after amount/wallet/due/category দেখাবে; ব্যবহারকারী edit/cancel/confirm করতে পারবেন।
4. Confirm-এর পর idempotency key সহ একক transaction-ে write; cloud parse ব্যর্থ হলে mutation fallback নয়।
5. অস্পষ্ট নির্দেশে প্রশ্ন/clarification; “প্রথম due” অনুমান করে mark-paid/delete নিষিদ্ধ।
6. Cloud context ন্যূনতম; financial data পাঠানোর আগে স্পষ্ট consent; release-এ debug logging নেই।

## ৮. বিল্ড ও রিলিজ পাইপলাইন

- সম্পূর্ণ Gradle wrapper; JDK/AGP/Kotlin/KSP/Compose সংস্করণ CI-তে sync করে লক।
- GitHub Actions workflow: checkout → setup JDK/Android SDK → cache → `./gradlew lint testDebugUnitTest assembleDebug` → release signing secret যাচাই → `assembleRelease` → R8 → `apksigner verify` → SHA-256 → artifact upload → tag/release হলে GitHub Release-এ APK+checksum+notes।
- Secrets: `HISHAB_KEYSTORE_BASE64`, `HISHAB_KEYSTORE_PASSWORD`, `HISHAB_KEY_ALIAS`, `HISHAB_KEY_PASSWORD`। Secret না থাকলে release job fail-fast; debug APK কখনো release হিসেবে প্রকাশ নয়।
- Keystore ব্যবহারকারী নিজের ডিভাইসে offline তৈরি করবেন; আমি private key/password চাইব না, chat/repo-তে রাখব না। Secure backup guide README-তে।
- Release artifact: `hishab-v1.0.0-release.apk` + `.sha256`; applicationId `com.hishab.app`; minSdk 29; target/compile SDK 36 স্থিতিশীলতা যাচাই সাপেক্ষে।
- এই sandbox-এ JDK/Android SDK নেই; compile/emulator যাচাই GitHub Actions-এ হবে। Local-এ static/unit-level যাচাই যতটা সম্ভব।

## ৯. টেস্ট ও যাচাই গেট

- Unit: money parser/formatter, accounting dual logic, due partial/overpay, transfer neutrality, budget thresholds, date/month/timezone, CSV injection escaping, backup round-trip/validation, AI draft no-write।
- Room: DAO/in-memory, migration v1, delete/archive constraints, idempotent todo conversion।
- UI/Robolectric/instrumented যতটা CI-তে সম্ভব: entry validation, sheet keyboard, dark/light, font scale, drawer back stack, confirm dialogs, empty state।
- Manual device checklist: TalkBack, 48dp targets, notification permission, Calendar fallback, SAF import/export, signed APK install/update।
- Release gate: lint+unit+build পাস, signature verify, checksum মিলবে; ব্যর্থ হলে release প্রকাশ বন্ধ।

## ১০. Cleanup ও README (সব কাজের পরে)

- মুছবে: nested legacy ZIP, duplicate WebView/API, `public/assets/aistudio` prompt, debug logs, অপ্রযোজ্য AI Studio metadata/blueprint/README, unused resources/deps, placeholder tests।
- রাখবে: নেটিভ সোর্স, design docs, font OFL, verified wrapper/workflow, প্রয়োজনীয় টেস্ট।
- নতুন README: বাংলা+ইংরেজি শিরোনাম, badges, স্ক্রিনশট, ফিচার, হিসাব নীতি, privacy, build/run, signed release guide, troubleshooting, roadmap, license/attribution। আকর্ষণীয় কিন্তু অতিরঞ্জিত দাবি ছাড়া।
- মূল Google Drive ZIP অপরিবর্তিত থাকবে; `.arena/` ও `google_drive/` commit হবে না।

## ১১. বাস্তবায়ন ক্রম

1. অনুমোদন → সোর্স repo-তে আনা + package/brand rename + wrapper/build স্থির করা।
2. Domain/Room/DataStore ভিত্তি + money/accounting tests।
3. Repository/ViewModel/navigation/theme/design system।
4. Home/entry/transactions/categories/wallets/dues/payments/todos/budgets/reports/settings/backup/reminders/AI-confirm।
5. Bugfix regression tests + CI green।
6. Signed release workflow + secrets guide + release।
7. Dead-file cleanup + README + final verification।

## ১২. এখন যেসব অনুমোদন দরকার

- সামগ্রিক মকআপ ও এই পরিকল্পনা অনুমোদন।
- ঐচ্ছিক লেগ্যাসি/রোডম্যাপ ফিচার: কোন সেট v1-এ ঢুকবে (swipe/undo, icon picker, item insight, app lock, Drive backup)। ডিফল্ট প্রস্তাব: প্রথম তিনটি অন্তর্ভুক্ত, app lock ও Drive backup defer।
