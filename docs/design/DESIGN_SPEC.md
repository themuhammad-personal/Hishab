# হিসাব / Hishab — ডিজাইন স্পেসিফিকেশন

**স্ট্যাটাস:** অনুমোদনের জন্য প্রস্তাবিত। এটি কার্যকরী অ্যাপ কোড নয়।  
**মকআপ:** `docs/design/hishab-mockups.html` (স্বয়ংসম্পূর্ণ, ফন্ট base64-embedded, কোনো CDN নেই)  
**প্রিভিউ:** `docs/design/preview/hishab-overview.png` + ৪টি বোর্ড + ১২টি individual screen PNG  
**ভেরিফিকেশন:** Chromium headless render; ৪টি ফন্ট weight loaded; horizontal/vertical overflow নেই; page error নেই; external request নেই; মূল text/background contrast ≥ 4.98:1 (light) ও ≥ 6.74:1 (dark semantic pairs)।

---

## ১. ডিজাইন নীতি

1. **খাতা আগে, সাজসজ্জা পরে।** সংখ্যা, তারিখ, বাকি ও wallet balance এক নজরে বোঝা যাবে।
2. **দ্বৈত হিসাব দৃশ্যমান।** “মাসের খরচ” বাকিসহ; “ওয়ালেট ব্যালেন্স” শুধু প্রকৃত পরিশোধে। দুটি আলাদা কার্ড/লেবেল।
3. **এক হাতে ব্যবহারযোগ্য।** প্রধান action FAB; entry/edit/payment bottom sheet; গুরুত্বপূর্ণ বাটন ≥ ৪৮dp। [1](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
4. **Drawer প্রধান navigation।** ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী bottom nav বাধ্যতামূলক নয়। হোম থেকে দ্রুত shortcut: নতুন হিসাব, বাকি, ওয়ালেট, বাজেট, রিপোর্ট। বড় স্ক্রিনে expanded drawer/rail বিবেচনা। [5](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns)
5. **Offline-first বিশ্বাস।** লোকাল/ক্লাউড অবস্থা স্পষ্ট; sync না হলে “সিঙ্ক হয়নি” দেখাবে, কখনো ভুয়া success নয়।
6. **AI খসড়া দেয়, সিদ্ধান্ত নেয় না।** প্রতিটি mutation-এর আগে preview + নিশ্চিতকরণ।

## ২. ভিজ্যুয়াল টোকেন

| Token | Light | Dark | ব্যবহার |
|---|---:|---:|---|
| paper/background | `#F7F8F3` | `#101C17` | scaffold |
| surface | `#FFFFFF` | `#18271F` | card/sheet |
| surface-2 | `#EEF2EA` | `#21342A` | chip/segment/track |
| ink | `#182C24` | `#EFF4EB` | primary text |
| muted | `#5F6F65` | `#B0C1B6` | secondary text |
| border | `#DFE6DC` | `#31463A` | divider/outline |
| primary | `#145443` | `#8ED4AF` | brand/action |
| on-primary | `#FFFFFF` | `#10261B` | primary button text |
| primary-soft | `#E2EEE6` | `#243E30` | selected chip |
| gold | `#C99B48` | `#E0B86B` | warning/budget 80% |
| gold-soft / ink | `#F7EBD4` / `#7A5518` | `#3A3020` / `#F0D49A` | warning banner |
| rose | `#A83E3E` | `#F0A3A3` | due/destructive |
| rose-soft | `#FBEAEA` | `#3E2929` | due tint |
| blue | `#2F628A` | `#A7C8E4` | info/bank |
| blue-soft | `#E6EEF5` | `#263744` | info tint |

গ্র্যাডিয়েেন্ট শুধু decorative hero-তে সীমিত; functional chart/progress-এ solid semantic color। Dark mode-এ hardcoded light tint নিষিদ্ধ।

## ৩. টাইপোগ্রাফি ও স্পেসিং

- Font: **Hind Siliguri** Regular/Medium/SemiBold/Bold; OFL license রাখা হবে।
- বাংলা default; Settings-এ English digits toggle। সংখ্যা formatter এক জায়গায়।
- Scale (sp): display 36–38, headline 24–28, title 20, section 16, body 14, label 12–13, caption 10–11। Compose-এর non-linear font scaling বন্ধ করা হবে না; ২০০% scale-এ layout test।
- Spacing: 4/8/12/16/20/24/32। Card radius 18–24, sheet radius 28, button radius 14, chip radius 12।
- Touch target ≥ ৪৮dp; ঘন তালিকায় icon button ৪৮dp, row padding দিয়ে hit area বাড়ানো।

## ৪. হিসাবের নিয়ম (UI-তে যেভাবে দেখাবে)

- `মাসের খরচ = পরিশোধিত খরচ + বাকি খরচ`
- `ওয়ালেট ব্যালেন্স = প্রারম্ভিক ব্যালেন্স + পরিশোধিত আয় − পরিশোধিত খরচ + নিট স্থানান্তর`
- `স্থানান্তর` আয়/খরচ নয়; মোট ব্যালেন্স অপরিবর্তিত।
- `বাকি পরিশোধ` নতুন খরচ নয়; সংশ্লিষ্ট due-এর against-এ payment history যুক্ত হবে, wallet কমবে, মাসের খরচ অপরিবর্তিত।
- Budget warning: ৮০% ও ১০০% in-app; spending block করবে না।
- নমুনা মকআপ হিসাব: আয় ৳৩৫,০০০; খরচ ৳১৮,৪৫০ = পরিশোধিত ৳১৫,২০০ + বাকি ৳৩,২৫০; প্রারম্ভিক ৳৯,০০০; wallet ৳২৮,৮০০।

## ৫. স্ক্রিন ম্যাপ

| # | Screen | মকআপ | মূল বিষয় |
|---|---|---|---|
| 1 | Home light | `screen-home.png` | বাকিসহ খরচ, আয়, wallet, budget strip, recent |
| 2 | Entry sheet | `screen-entry.png` | expense/income/transfer, category, wallet, date, paid/baki, quantity × unit price, note |
| 3 | Home dark | `screen-home-dark.png` | semantic dark tokens |
| 4 | Dues | `screen-dues.png` | দেনা/পাওনা, partial progress, history, reminder |
| 5 | Payment sheet | `screen-payment.png` | before→after due/wallet preview, মাসের খরচ অপরিবর্তিত |
| 6 | Wallets | `screen-wallets.png` | total, per-wallet, edit, transfer, archive |
| 7 | Budget | `screen-budget.png` | monthly + category, 80/100 warning, non-blocking |
| 8 | Reports | `screen-reports.png` | exact donut sweep, largest-remainder %, daily bars, CSV |
| 9 | Drawer | `screen-drawer.png` | primary nav, local status, due count |
| 10 | AI confirm | `screen-ai.png` | local mode, typed draft, cancel/edit/confirm |
| 11 | Settings | `screen-settings.png` | theme, digits, reminder, JSON/CSV/restore, cloud/AI opt-in |
| 12 | Empty start | `screen-empty.png` | no auto-seed, first entry, wallet/category setup |

## ৬. কম্পোনেন্ট চুক্তি

- `MoneyText`: poisha → locale digits; negative/zero/format consistent।
- `AccountingSummaryCard`: expense paid/unpaid split; wallet আলাদা।
- `EntrySheet`: expandable; validation field-level; save sticky; keyboard ime-aware।
- `DueCard`: payable/receivable, original/paid/remaining, due date, history, pay action।
- `PaymentPreview`: before→after; mutation ছাড়া preview।
- `BudgetProgress`: normal/warn/full; semantic color + text label (color-only নয়)।
- `ConfirmMutationDialog/Sheet`: AI ও manual destructive action একই safety pattern।
- `SyncStatusBadge`: local / syncing / synced / failed / disabled; মিথ্যা success নিষিদ্ধ।

## ৭. আইকন ও ব্র্যান্ড

- বিদ্যমান বই + ৳ + পাতা পরিচয় পরিমার্জন; ছোট আকারে wordmark বাদ।
- Adaptive icon: foreground vector mark, background emerald, monochrome themed icon Android 13+।
- মকআপ লোগো inline SVG; অ্যাপে vector drawable/adaptive icon resource হবে।

## ৮. অ্যাক্সেসিবিলিটি ও যাচাই

- TalkBack label: amount, status, due date, wallet name; decorative icon `contentDescription=null`।
- Color-এর পাশাপাশি text/icon দিয়ে status বোঝানো।
- Font scale ১০০/১৫০/২০০%, landscape/tablet, keyboard open, dark mode screenshot regression।
- মকআপ static; বাস্তব Compose UI-তে আলাদা instrumented/Robolectric যাচাই প্রয়োজন।
