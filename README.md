# FreePay Sync (Android)

The SMS-forwarding companion app for [FreePay](https://github.com/lagadev/freepay).
Install it on the phone that receives your brand's bKash/Nagad/Upay/Rocket/
Cellfin confirmation SMS — it reads them, parses the transaction ID +
amount, and forwards them to your FreePay backend (`POST /api/sms/ingest`)
using your **Brand API Key**.

## How it's built

There's no local Android SDK involved — a GitHub Actions workflow
(`.github/workflows/release.yml`) builds the APK on GitHub's own runners and
publishes it as a **GitHub Release** with `freepay-sync.apk` attached.

Trigger it either by:
- Pushing a tag like `android-v1.0.0`, or
- Going to the repo's **Actions** tab → "Build FreePay Sync APK" → **Run workflow**

The build produces a **debug-signed APK** (fine for direct sideload install;
not for the Play Store). Download it from the release's Assets section.

## Using the app

1. Install the APK on the phone that will receive the SMS (needs "install
   from unknown sources" allowed for whichever app you download it with).
2. Open the app → paste your **FreePay Server URL** (your deployed Worker's
   URL) and your **Brand API Key** (from the FreePay dashboard's Brand page).
3. Tap Login — the app calls `GET /api/brands/me` to confirm the key is
   valid and shows which brand it's connected to.
4. Grant SMS permission when prompted.
5. Leave the app installed and the phone online — incoming bKash/Nagad/
   Upay/Rocket/Cellfin "received" SMS get parsed and forwarded automatically,
   and show up in the app's own list with a live status: PENDING → SYNCED /
   MATCHED, or FAILED if the upload didn't go through (WorkManager retries
   automatically once the network is back).
6. A persistent banner in the app links straight to **@devugly** on Telegram
   for support.

## Tuning SMS parsing

`SmsParser.kt` has the regex patterns for each wallet's "money received"
message. bKash and Nagad have dedicated patterns; Upay/Rocket/Cellfin use a
generic fallback. Real-world wording can drift — if a wallet's SMS isn't
being picked up, test with a real message and adjust its pattern.
