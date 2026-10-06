# Civics Voice
> *"The Voice and Needs of the People"*

Civics Voice is a civic grievance management platform connecting citizens directly with municipal authorities. It provides end-to-end grievance tracking, interactive GPS location tagging via Google Play Services, CameraX photo evidence, AI-powered triage, audio transcription, and zero-trust verification.

---

## 🌟 Key Features

### 1. Dual-Role Architecture (Citizen & Municipal Authority)
- **Public Citizen:**
  - Submit civic complaints via text, speech/microphone, and photo proof.
  - **CameraX Photo Capture**: In-app live camera capture (`CameraCapture` component) with lens switching and shutter controls, plus Android Photo Picker integration for gallery media.
  - **Interactive Location Picker (`play-services-location`)**: High-accuracy GPS tagging (`FusedLocationProviderClient`) with a pan/zoom map canvas, center defect pin, reverse geocoding, and municipal ward landmark shortcuts.
  - Real-time audit trail and status tracking.
  - **Citizen Verification:** When authorities resolve an issue, the citizen confirms whether the fix was satisfactory (`YES` to close, `NO` to reopen).
  - Neighborhood community upvoting / support count.
- **Municipal Authority:**
  - Real-time KPI metrics dashboard (Submitted, In Progress, Citizen Verification, Closed, Reopened).
  - Search, filter by department and priority.
  - Officer assignment and field resolution notes with status transitions.
- **Demo Mode:**
  - Seamless one-tap toggle between Citizen and Authority roles for review and evaluation.

### 2. Cutting-Edge Gemini AI Capabilities
- **Live Conversational AI (`gemini-3.8-live`):**
  - Real-time voice assistance for civic inquiries, ward policies, and helpline status.
- **Microphone Audio Transcription (`gemini-3.5-transcribe`):**
  - Transcribes voice complaint submissions supporting English, Tamil, and Tanglish.
- **High Thinking Mode (`gemini-3.1-pro-preview`):**
  - Deep urban planning, multi-department statutory reasoning, and liability analysis with `ThinkingLevel.HIGH`.
- **Smart Triage & Duplicate Detection:**
  - Analyzes complaint descriptions to route issues to the correct department (EB, Water, Sanitation, PWD), estimates priority, and detects neighborhood duplicates.

### 3. Enterprise Backend & Security
- **Firebase Authentication:** Google Sign-In via Jetpack Credential Manager.
- **Cloud Firestore:** Real-time listeners for live tracking and status timeline.
- **Zero-Trust Security Rules:** Strict schema validation in `firestore.rules`.
- **Material 3 Design:** Branded Government-Tech palette (Teal `#14B8A6`, Deep Blue `#0B3C8C`, Green `#22A06B`).

---

## 🏗️ Technical Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM + Clean Architecture
- **State Management:** Kotlin Coroutines, StateFlow
- **Networking:** OkHttp, Retrofit
- **Image Loading:** Coil
- **Location Services:** Google Play Services FusedLocationProviderClient

---

## 📱 Quick Walkthrough for Testers & Evaluators

1. **Launch App:** Select "Sign in with Google" or choose "Enter as Public Citizen" / "Enter as Municipal Authority" for quick demo access.
2. **Report Grievance:** Tap **Report Issue** or the Floating Action Button.
   - Enter complaint description or tap **Speak (Tamil/English)** to transcribe via `gemini-3.5-transcribe`.
   - Tap **Use GPS** to pinpoint coordinates.
   - Attach photo evidence using Android's Photo Picker.
   - Tap **Submit Complaint**.
3. **Live Tracking:** Open **Live Tracking** to view the audit trail timeline and AI triage score.
4. **Authority Mode:** Tap the role switch icon in the top bar to switch to Authority Mode.
   - Open **Authority Hub** to see real-time metrics.
   - Advance complaint status to **In Progress** or **Mark Resolved** with officer notes.
5. **Citizen Verification:** Return to Citizen mode to review the resolution. Tap **YES, Resolved** to close or **NO, Reopen** to request further field action.
6. **Live AI & Deep Thinking:** Try **Live Voice AI** (`gemini-3.8-live`) or **Deep Thinker** (`gemini-3.1-pro-preview` with High Thinking) from the home grid.
