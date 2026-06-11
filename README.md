# EduQuiz

[![License](https://img.shields.io/badge/license-Proprietary-blue)](LICENSE)
[![Python](https://img.shields.io/badge/python-3.10%2B-informational)](#)
[![FastAPI](https://img.shields.io/badge/FastAPI-ready-success)](#)

**EduQuiz** is a Smart HSC Science MCQ Practice App with an integrated FocusGuard mechanism and a cyberpunk neon UI. It converts scanned or uploaded PDF textbooks into Bengali MCQs using a hybrid pipeline: a PyMuPDF streaming extractor and optional Google Gemini model validation/selection for high-quality MCQs.

**Repository layout (key paths)**
- Frontend: [client/src/components/UploadDashboard.tsx](client/src/components/UploadDashboard.tsx)
- Backend AI engine: [ai_engine/main.py](ai_engine/main.py)
- Parser utilities: [assets/parser.py](assets/parser.py)
- Android manifest: [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)
- Android Gradle: [app/build.gradle.kts](app/build.gradle.kts)

**Quick features**
- Cyberpunk Neon UI with a dedicated Bengali PDF picker button labeled "পিডিএফ ফাইল সিলেক্ট করুন 📂" and dynamic selected-file display.
- Custom book launcher icon support (`app_icon`) wired into the Android manifest.
- Streamed PDF extraction via `PyMuPDF (fitz)` to avoid RAM spikes on large textbooks.
- Intelligent MCQ extraction pipeline using the Google Gemini model (`gemini-1.5-flash`) with strict JSON output constraints and a robust fallback to parser heuristics.
- Local caching of processed PDFs using SHA-256 checksums to avoid duplicate work.

**Why this project exists**

It automates the conversion of HSC science textbooks (Bangla + English mixed content) into high-quality MCQ practice sets for students while keeping the mobile UI responsive and secure.

----

**Table of contents**

- **Project Overview & Features**
- **Tech Stack Architecture**
- **Environment Configuration (.env)**
- **Local Development Setup**
- **Production & Permissions**
- **Android Icon Instructions**
- **Contributing & Support**

----

**Project Overview & Features**

EduQuiz is a mobile+web hybrid product designed for HSC Science learners. Major capabilities include:

- **PDF → MCQ pipeline:** Upload a PDF via the frontend and the backend extracts text page-by-page (memory-safe) and returns structured Bengali MCQs.
- **FocusGuard:** Native UI overlay to help students concentrate while taking practice tests.
- **Caching & Deduplication:** Files are hashed (SHA-256) and cached to prevent repeated processing of the same content.
- **Multilingual OCR & Parsing:** The extractor preserves Bangla UTF-8 text, vowel signs, and scientific notations; OCR fallback via `easyocr` exists for images.
- **Gemini-driven QA:** Optionally uses `gemini-1.5-flash` to validate and select the cleanest MCQs and output strict JSON arrays.

----

**Tech Stack Architecture**

- **Frontend:** React + TypeScript, Vite, TailwindCSS (Cyberpunk Neon Theme). See `client/src/components/UploadDashboard.tsx` for the upload component implementation.
- **Backend Engine:** Python, FastAPI, Uvicorn, PyMuPDF (`fitz`), EasyOCR (optional), Google Generative AI SDK (`google-generativeai`) for Gemini calls. See `ai_engine/main.py`.
- **Android Core:** Native Android shell (Gradle Kotlin DSL), Java/Kotlin interoperability, Room SQLite for local state/cache, Retrofit + OkHttp for HTTP networking. Build and dependency resolution use the version catalog at `gradle/libs.versions.toml`.

----

**Environment Configuration (.env)**

Create a `.env` in the project root (not committed). Example file: `.env.example` (already provided).

Frontend (Vite) variables — place in `client/.env` or in the root if using shared secrets plugin:

```
VITE_API_URL=http://127.0.0.1:8000/process-pdf
```

Backend variables — place in the project root `.env` or export in your shell environment:

```
GEMINI_API_KEY=your_google_gemini_api_key_here
```

Security notes:
- Never commit `.env` to git.
- For CI or production, configure environment secrets using your CI/CD provider's secret management.

----

**Step-by-Step Local Development Installation Guide**

Backend (Python / FastAPI)

1. Create a Python virtual environment and activate it (recommended Python 3.10+):

```bash
python -m venv .venv
source .venv/bin/activate
```

2. Install backend dependencies:

```bash
pip install -r ai_engine/requirements.txt
```

3. Export or set your Gemini API key in the shell (or use `.env`):

```bash
export GEMINI_API_KEY="your_key_here"
```

4. Run the FastAPI server (development):

```bash
# from repo root
uvicorn ai_engine.main:app --reload --host 0.0.0.0 --port 8000
```

Frontend (React / Vite)

1. Change into the client folder and install dependencies:

```bash
cd client
npm install
```

2. Start the Vite dev server (ensure `VITE_API_URL` points to your backend):

```bash
npm run dev
```

3. The upload component is implemented at [client/src/components/UploadDashboard.tsx](client/src/components/UploadDashboard.tsx) and will POST a multipart/form-data request with the PDF file and `subject_name` to the configured API URL.

Android Build (Gradle)

1. Ensure Java 11+ and Android SDK/Android Studio are installed and configured.

2. From the repository root, build a debug APK:

```bash
./gradlew :app:assembleDebug
```

3. Install the built APK on an emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

----

**Production & Permission Details**

- The Android manifest at [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) has been updated to include runtime network permissions and to allow cleartext traffic for local testing:

	- `<uses-permission android:name="android.permission.INTERNET" />`
	- `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
	- `android:usesCleartextTraffic="true"` is set on the `<application>` tag for local HTTP testing (e.g., `http://10.0.2.2:8000`).

- For production builds, you should remove `usesCleartextTraffic` or set network security config to only allow secure endpoints and enable HTTPS for APIs.

----

**Android App Icon**

The app manifest now points to `@mipmap/app_icon`. To apply your icon image, place appropriately-scaled PNGs in the following folders:

- `app/src/main/res/mipmap-mdpi/app_icon.png` (48x48)
- `app/src/main/res/mipmap-hdpi/app_icon.png` (72x72)
- `app/src/main/res/mipmap-xhdpi/app_icon.png` (96x96)
- `app/src/main/res/mipmap-xxhdpi/app_icon.png` (144x144)
- `app/src/main/res/mipmap-xxxhdpi/app_icon.png` (192x192)

Use Android Studio's Image Asset tool for adaptive icons (recommended).

----

**Developer Notes & Troubleshooting**

- If `GEMINI_API_KEY` is missing, `ai_engine/main.py` will return a 500 error explaining it is not configured.
- The backend uses a streaming-to-disk approach for uploaded PDFs — ensure the environment has sufficient disk space for temporary processing.
- The Gemini call expects strictly formatted JSON in the response text. If the model returns non-JSON wrappers, the backend tries to extract the JSON array substring before `json.loads()`.
- To test locally without Gemini, you can comment out the Gemini block and rely on the `assets/parser.py` heuristic parser.

----

**Contributing & Support**

- For bug reports and feature requests, open an issue in this repository.
- To contribute code: fork → create feature branch → submit PR with clear description and tests.

----

**License & Acknowledgements**

This repository includes third-party libraries (FastAPI, PyMuPDF, EasyOCR, google-generativeai). Follow their licenses as required.

----

If you'd like, I can also:

- Add CI workflows to run linting and unit tests.
- Create a small demo PDF and a smoke-test script that exercises the whole pipeline.

---

Generated and updated to reflect the current implementation.
