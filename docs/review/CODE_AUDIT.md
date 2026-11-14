# Hishab / হিসাব — প্রি-ইমপ্লিমেন্টেশন কোড অডিট

**তারিখ:** ২০২৬-১১-১৪  
**উৎস:** Google Drive `untitled (1).zip`  
**SHA-256:** `220acd6bce5938d6f6e755ae216b4cc9876a5786adff9f4c851ec2d5169efca7`  
**অবস্থা:** রিভিউ সম্পন্ন; ইমপ্লিমেন্টেশন এখনো শুরু হয়নি। ব্যবহারকারীর অনুমোদনের জন্য মকআপ ও পরিকল্পনা প্রস্তুত।

---

## ১. স্কোপ ও পদ্ধতি

- ZIP-এ ১০৮টি এন্ট্রি; CRC ও নিরাপদ extraction যাচাই করা হয়েছে।
- মূল নেটিভ প্রজেক্ট: Kotlin + Jetpack Compose, ২৫টি Kotlin ফাইল, প্রায় ৮,১৪৩ লাইন।
- ভেতরের লেগ্যাসি প্রজেক্ট: Next.js ওয়েব অ্যাপ + raw Android WebView + একটি duplicate Android ZIP।
- ৯৪টি text/config ফাইল লাইন-বাই-লাইন স্ক্যান; সব Kotlin UI/data/AI ফাইল ম্যানুয়ালি পড়া; লেগ্যাসি HTML/JS/API/workflow রিভিউ; JSON/XML parse ও JS syntax চেক করা হয়েছে।
- বাইনারি আইকন/ফন্ট inspect করা হয়েছে। ফন্টের লাইসেন্স ফাইল মূল প্যাকেজে নেই; upstream OFL পাওয়া গেছে।
- **সীমাবদ্ধতা:** এই sandbox-এ JDK/Android SDK নেই। তাই APK compile, emulator run, Firebase live integration বা device testing এখনো করা যায়নি। কোনো বাগ “ফিক্সড” বলা হচ্ছে না।

---

## ২. ব্যবহারকারীর চূড়ান্ত সিদ্ধান্ত

| বিষয় | সিদ্ধান্ত |
|---|---|
| ভিত্তি | Kotlin + Compose নেটিভ; দুই সংস্করণের সেরা প্রয়োজনীয় ফিচার |
| পুরনো ডেটা | নতুন করে শুরু; legacy migration বাধ্যতামূলক নয় |
| Auth | Local-first; Google ঐচ্ছিক |
| Firebase | AI Studio project access অনিশ্চিত; প্রথম রিলিজ local, পরে যাচাই করে sync |
| ফিচার | Bugfix + UI + budget, wallet management, reminder, undo, full backup, CSV |
| হিসাব নীতি | মাসের খরচে বাকি খরচও থাকবে; wallet balance শুধু প্রকৃত পরিশোধে কমবে |
| নাম | Hishab / হিসাব |
| লোগো | বর্তমান বই-ও-পাতা পরিচয় পরিমার্জন |
| ভিজ্যুয়াল | Paper + Emerald পরিমার্জিত |
| Navigation | Drawer প্রধান; bottom navigation নয় |
| ভাষা/থিম | বাংলা default; English digits ও light/dark/system toggle |
| এন্ট্রি ফর্ম | Expandable bottom sheet |
| বাকি | দেনা + পাওনা + আংশিক পরিশোধ + history |
| Wallet | পূর্ণ management + transfer; transfer আয়/খরচ নয় |
| Budget | In-app সতর্কতা; notification বাধ্যতামূলক নয় |
| Reminder | Local notification + Calendar shortcut |
| Export | পূর্ণ JSON + CSV |
| AI | সব পরিবর্তনের preview/confirmation; cloud পাঠানোয় আলাদা consent |
| Cloud target | Local-first; Firebase পরে যাচাই |
| Application ID | `com.hishab.app` |
| Distribution | GitHub Releases signed APK |
| Signing | নতুন release key; private key/password repo বা chat-এ নয় |
| Minimum Android | Android 10+ / API 29 |
| Privacy | Analytics/advertising SDK নয় |

---

## ৩. গুরুত্বপূর্ণ ফাইন্ডিংস

Severity: **P0** = ডেটা/নিরাপত্তা/মূল কাজ ভাঙে, **P1** = গুরুতর নির্ভরযোগ্যতা/UX, **P2** = মান/রক্ষণাবেক্ষণ।

### ৩.১ Build, release ও platform

| ID | Severity | প্রমাণ | সমস্যা | পরিকল্পিত সমাধান |
|---|---|---|---|---|
| B01 | P0 | `app/src/main/AndroidManifest.xml:5-6` | `android.intent.permission.INTERNET` ও `...ACCESS_NETWORK_STATE` ভুল namespace। Firebase/AI/network কাজ নাও করতে পারে। | সঠিক `android.permission.*`; manifest merge test। |
| B02 | P0 | `gradle/wrapper/` | শুধু `gradle-wrapper.properties`; `gradlew`, `gradlew.bat`, wrapper JAR নেই। | সম্পূর্ণ verified wrapper; CI-তে `./gradlew` ব্যবহার। |
| B03 | P0 | `app/build.gradle.kts:27-50` | Release signing absent keystore/env-এর ওপর নির্ভরশীল। | নতুন key secure workflow-এ; secret ছাড়া release job fail-fast। |
| B04 | P1 | `app/build.gradle.kts:35-39,52-55` | Debug build custom `debug.keystore` চায় কিন্তু ফাইল নেই। | standard debug signing বা deterministic local generation। |
| B05 | P1 | `app/build.gradle.kts:116-117` | App Check debug provider release dependency হিসেবে আছে। | debug-only source/dependency; release provider আলাদা। |
| B06 | P1 | `MainActivity.kt:149-160`, `firebase_applet_config.xml` | Release-এ reCAPTCHA site key/provider sideloaded APK-এর জন্য যাচাই করা হয়নি। | Firebase সক্রিয় করার আগে Play Integrity/outside-Play configuration যাচাই; local release-এ fake sync/AI না দেখানো। |
| B07 | P2 | `README.md`, `.env.example`, secrets plugin | AI Studio নির্দেশনা ও বর্তমান signing কোড অসংগত; `.env.example`-এ প্রকৃত key নেই। | নতুন README/setup; অপ্রয়োজনীয় secrets plugin সরানো। |
| B08 | P2 | `app/build.gradle.kts:46` | Release minify বন্ধ। | R8 rules সহ shrink/test; startup ও reflection regression check। |

### ৩.২ ডেটা, হিসাব ও backup

| ID | Severity | প্রমাণ | সমস্যা | পরিকল্পিত সমাধান |
|---|---|---|---|---|
| D01 | P0 | `GhorkhataRepository.kt:59-127` | নতুন ব্যবহারকারীর আসল খাতায় sample transaction/todo auto-seed হয়। | প্রথম রিলিজ পরিষ্কার খাতা; sample শুধু আলাদা demo mode/স্পষ্ট action। |
| D02 | P0 | `GhorkhataRepository.kt:181-195`; `AppSettingsView.kt:495-513` | “সব মুছুন” বললেও শুধু transactions/todos মোছে; category/wallet/settings নয়। ৫০০-এর বেশি হলে single batch fail করবে। | scoped reset, chunked delete, confirmation, backup-first prompt। |
| D03 | P0 | `GhorkhataRepository.kt:426-469` | Backup-এ categories/wallets/settings নেই; transaction `dueDate`, todo `dueDate` বাদ পড়ে। | versioned full schema; round-trip test। |
| D04 | P0 | `GhorkhataRepository.kt:472-532` | Import-এ schema/amount/date validation নেই; `NaN` ঢুকতে পারে; single batch limit; createdAt overwrite; merge/replace preview নেই। | validate-first, preview, atomic/chunked import, idempotency, rollback strategy। |
| D05 | P0 | `HomeDashboardView.kt:68-96`, `ReportsView.kt:78-100`, `GeminiAiService.kt:78-80` | Expense-এ baki/pending যুক্ত কিন্তু wallet-এ শুধু paid; AI আবার শুধু `categoryId == income` ধরে। | একক domain calculator; category type authoritative; accrued expense বনাম cash balance স্পষ্ট। |
| D06 | P1 | `Models.kt` সর্বত্র | টাকা `Double`; rounding drift ঝুঁকি। | integer poisha (`Long`) + decimal parser/formatter; property tests। |
| D07 | P1 | `BnFormat.kt:82-129` | invalid date/current-month fallback নীরব; timezone/calendar ভিত্তিক মাস পরিবর্তন। | `java.time`, strict ISO validation, device timezone policy, DST/month-end tests। |
| D08 | P1 | `GhorkhataRepository.kt:255-280,329-350` | Error/empty হলে default categories/wallets ফিরে আসে; ব্যবহারকারী deleted মনে করতে পারেন। | loading/empty/error আলাদা; default শুধু explicit initialization। |
| D09 | P1 | `GhorkhataRepository.kt:258-280,356-373` | Delete/update error swallow করে UI success ধরে নিতে পারে। | Result/state propagation, retry, snackbar/undo। |
| D10 | P1 | `TransactionModal.kt:121,318,619` | `dokan_baki` ID চেক, আসল seed ID `dues`; baki auto-select ভাঙে। | category type দিয়ে logic; migration/alias test। |
| D11 | P1 | `MainActivity.kt:593-603,657-667` | Todo → expense conversion-এ link/idempotency নেই; বারবার চাপলে duplicate expense। | conversion ID, atomic mark-converted, duplicate guard। |
| D12 | P1 | `CategoryManagerModal.kt:122,144-147` | Category delete orphan transaction তৈরি করে; edit dialog state পুরনো থাকতে পারে। | reassign/archive flow; keyed form state। |
| D13 | P2 | `TransactionModal.kt:125,667-686,720` | Todo priority UI আছে কিন্তু model/save-এ নেই; target category সর্বদা bazar। | persist priority/target/date; অথবা অকার্যকর UI সরানো। |
| D14 | P2 | `SeedData.kt`, `AppSettingsView.kt` | Wallet CRUD UI নেই; budget field থাকলেও alert/limit ব্যবহার নেই। | wallet manager, transfer, budget calculator ও in-app alert। |
| D15 | P1 | `AppSettingsView.kt:99-106` | Backup `EXTRA_TEXT` হিসেবে share হয়; বড় খাতায় fail/truncate ঝুঁকি। | Storage Access Framework file export/import; MIME ও size guard। |

### ৩.৩ AI ও নিরাপত্তা

| ID | Severity | প্রমাণ | সমস্যা | পরিকল্পিত সমাধান |
|---|---|---|---|---|
| A01 | P0 | `GeminiAiService.kt:170-194,245-276` | অস্পষ্ট/ভুল নির্দেশেও প্রথম matching বা প্রথম due mark-paid/delete হতে পারে। | কোনো auto-mutation নয়; typed draft + user confirmation। |
| A02 | P0 | `GeminiAiService.kt:117-217` | Cloud action আংশিক execute হওয়ার পর exception হলে offline fallback একই কাজ আবার করতে পারে। | parse/execute আলাদা; idempotency key; fallback শুধু parsing/analysis-এ। |
| A03 | P1 | `GeminiAiService.kt:86-101` | Financial context cloud-এ পাঠানো হয়; আলাদা consent নেই। | opt-in cloud AI; ন্যূনতম প্রয়োজনীয় context; offline default। |
| A04 | P1 | `GeminiAiService.kt:454-510` | Offline parser amount/category ভুল হতে পারে; শেষ সংখ্যা amount ধরে; validation নেই। | conservative parser, confidence, preview, editable fields। |
| A05 | P2 | `GeminiAssistantView.kt:337-345` | Markdown `**` Compose Text-এ raw দেখায়। | নিরাপদ rich-text renderer বা plain formatting। |
| A06 | P2 | `GeminiAssistantView.kt:98-104` | Chat state navigation/rotation-ে হারায়। | ViewModel/state restoration। |

### ৩.৪ UI, accessibility ও UX

| ID | Severity | প্রমাণ | সমস্যা | পরিকল্পিত সমাধান |
|---|---|---|---|---|
| U01 | P1 | `HeroSummaryCard.kt:150,157,584`; theme files | Dark mode-এ hardcoded dark ink/light tint; contrast ভাঙে। | semantic tokens; light/dark contrast test। |
| U02 | P1 | `TransactionItemRow.kt:236-260`, drawer/month controls | বহু tap target ২৮–৪০dp; accessibility guideline-এর নিচে। | ন্যূনতম ৪৮dp target; ঘন layout-এ spacing। [1](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) |
| U03 | P1 | `TransactionModal.kt:160-170`, category/todo dialogs | fixed/max height + keyboard clipping ঝুঁকি; save validation message নেই। | ime-aware bottom sheet, sticky action, field-level error। |
| U04 | P1 | Transaction/category/todo delete paths | Undo বা যথেষ্ট confirmation নেই। | confirm destructive bulk action; single delete-এ undo window। |
| U05 | P1 | `MainActivity.kt:239-250` | screen/modal/month state `remember`; process death-এ হারায়; lifecycle-aware collection নয়। | ViewModel + SavedStateHandle/Navigation; `collectAsStateWithLifecycle`। [2](https://developer.android.com/topic/architecture/recommendations) |
| U06 | P2 | `MainActivity.kt` enum screen switch | proper back stack/deep link/navigation state নেই। | type-safe Navigation Compose; drawer primary থাকবে। |
| U07 | P2 | `ReportsView.kt:283-310,113-125` | ছোট donut segment বাদ পড়ে; rounded percentage যোগফল ১০০ নাও হতে পারে। | exact sweep, “অন্যান্য” grouping, largest-remainder percent। |
| U08 | P1 | `AppSettingsView.kt:110-118` | Calendar intent-এ begin/end time/reminder নেই; due date ব্যবহার হয় না। | per-item due date, all-day end exclusive, Calendar app fallback। |
| U09 | P2 | `Type.kt`, font asset | শুধু Regular weight; bold synthetic; OFL license অনুপস্থিত। | প্রকৃত weights + OFL; font-scale ২০০% test। |
| U10 | P2 | launcher PNG/JPG | ছোট আকারে লোগোর লেখা অস্পষ্ট; adaptive/themed icon নেই। | refined vector mark, adaptive/monochrome icon; ছোট আকারে text নয়। |

### ৩.৫ লেগ্যাসি ওয়েব/WebView

| ID | Severity | প্রমাণ | সমস্যা |
|---|---|---|---|
| L01 | P0 | `android/.../MainActivity.java:64`, `public/ghorkhata.html` | WebView-তে Google OAuth popup blocked; `/api/*` asset context-এ চলে না। |
| L02 | P1 | `public/ghorkhata.html:2395` | `renderCatChips()` কল আছে কিন্তু function নেই; আছে `renderModalCatChips()`। |
| L03 | P1 | `public/ghorkhata.html:2874-2877`, `app/api/calendar/route.ts:56` | paid status-এর বদলে বাংলা label compare; paid due-ও reminder হতে পারে। |
| L04 | P1 | `app/api/calendar/route.ts:23-33` | all-day start/end একই দিন; reminder override ভুল সময়ে পড়তে পারে। |
| L05 | P1 | `public/ghorkhata.html:2463-2490` | OAuth access token localStorage-এ সংরক্ষণ। |
| L06 | P1 | WebView `prompt()` handler নেই | client-id prompt কাজ করে না। |
| L07 | P1 | `.github/workflows/build-apk.yml:19-25` | নতুন `public/ghorkhata.html` পুরনো asset-এর ওপর copy করে feature regression ঘটায়। |
| L08 | P1 | legacy `android/app/build.gradle:17-23` | release debug key দিয়ে signed। |
| L09 | P2 | legacy WebView `setTextZoom(100)` | system font scaling উপেক্ষা। |
| L10 | P2 | duplicate HTML/ZIP/logs/prompt | একাধিক অসংগত কপি; রক্ষণাবেক্ষণ ও প্রকাশে ভুল ফাইল যাওয়ার ঝুঁকি। |

### ৩.৬ Firebase ও privacy

| ID | Severity | প্রমাণ | সমস্যা | পরিকল্পিত সমাধান |
|---|---|---|---|---|
| S01 | P1 | `firestore.rules:13-16,19-40` | `hasValidTimestamps()` সংজ্ঞায়িত করা কিন্তু ব্যবহার নেই; field/type/amount validation নেই। | rules validation + emulator tests; ownership বজায়। |
| S02 | P1 | public repo + `google-services.json` | Firebase project identifiers/config public; ownership/access অনিশ্চিত। | প্রথম local release-এ config প্রকাশ/নির্ভরতা এড়ানো; পরে verified project। |
| S03 | P2 | debug/firestore logs | অপ্রয়োজনীয় diagnostic log প্রকাশের ঝুঁকি। | release-এর আগে delete; log scrubbing। |
| S04 | P2 | backup/export | plaintext financial backup; CSV formula injection ভবিষ্যতে ঝুঁকি। | explicit warning, SAF, CSV cell sanitization; encryption ভবিষ্যৎ বিবেচনা। |

---

## ৪. কী এখনো যাচাই করা যায়নি

1. Gradle sync/compile: JDK/SDK নেই।  
2. Compose runtime crash/ANR: emulator নেই।  
3. Firebase Auth/App Check/Firestore live: project access ও credentials যাচাই হয়নি।  
4. Gemini cloud response: model `gemini-3.8-flash` বর্তমান Firebase docs-এ সমর্থিত দেখা গেছে, কিন্তু live quota/auth পরীক্ষা হয়নি। [3](https://firebase.google.com/support/releases)  
5. Signed APK install/update: নতুন key ও release workflow এখনো তৈরি হয়নি।  
6. Real-device TalkBack, keyboard, font-scale, notification permission: device test বাকি।

---

## ৫. ক্লিনআপ নীতি

ইমপ্লিমেন্টেশন ও টেস্ট পাসের **পরে** মুছবে:

- নেস্টেড `Gharkhata--main_*.zip` ও duplicate Android ZIP।
- `public/assets/aistudio/` master prompt ও অপ্রচলিত সম্পদ।
- `firebase-debug.log`, `firestore-debug.log`।
- AI Studio metadata/blueprint/README যা নতুন অ্যাপে অপ্রযোজ্য।
- অব্যবহৃত icon JPG, placeholder test, commented dependency, অকার্যকর legacy WebView/API।
- পুরনো README-এর বদলে নতুন আকর্ষণীয় README।

**রাখবে:** প্রয়োজনীয় Kotlin/Compose সোর্স, নতুন design docs, font OFL license, verified build/release config, প্রয়োজনীয় টেস্ট। মূল Drive ZIP অপরিবর্তিত থাকবে।

---

## ৬. গবেষণার ভিত্তি

- Android architecture: UI/data layer, repository, ViewModel, UDF, lifecycle-aware collection। [2](https://developer.android.com/topic/architecture/recommendations)
- Offline-first reference: Now in Android local source of truth ও WorkManager sync journey। [4](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)
- Accessibility: ন্যূনতম ৪৮dp touch target। [1](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- Adaptive navigation: drawer primary রাখা যাবে; বড় স্ক্রিনে rail/expanded drawer বিবেচনা। [5](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns)
- Firestore offline persistence ও last-write-wins সীমাবদ্ধতা। [6](https://firebase.google.com/docs/firestore/manage-data/enable-offline)
- App Check Play Integrity outside-Play configuration। [7](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
