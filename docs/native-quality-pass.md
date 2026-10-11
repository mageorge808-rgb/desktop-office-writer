# Native Android quality pass

This pass adds Ctrl+O/Ctrl+N while editing, a guarded Ctrl+Alt+T shortcut to the native Table Properties dialog, and preservation of WriterEditorActivity when the upstream document-switching path reopens its base class. Launcher buttons also use the dark palette.

The workflow attempts an Android 13 Google APIs x86_64 emulator on a standard free Ubuntu runner. ABI support is recorded and the real ARM64 APK must install and run; unsupported translation fails rather than passing a substitute. The smoke test uses a separately signed, test-only provider/instrumentation APK. Tests use a disposable DOCX and never user files. They open the engine, insert text and a table, save through a content URI, verify saved DOCX XML, then force-stop/reopen and check read-only mode. Emulator viewport bounds are 1280 × 854 density 160, approximately the Pad 7's aspect ratio. Actual Pad 7 resolution, input hardware and HyperOS behavior are not simulated fully.

The initial Android 11 run caught a redundant upstream WRITE_EXTERNAL_STORAGE gate: the manifest deliberately omits that permission, so the editor finished instead of using the existing scoped URI grant. The subclass now resumes the pinned private document loader after confirming the content URI remains readable. Real write grants still determine editability; no broad permission is claimed or granted. The packaging step checks that the pinned loader method exists. The old emulator's WebView 83 also requires a newer test image for modern editor JavaScript.

Native diagnostics record ABI support, measured load time, instrumentation output, screenshots and logcat. A failure prevents publication of the new installable artifact. The last successful APK remains available until its normal retention expires. This does not certify all Word features or matching Word layout. Rendering differences and actual document border visibility remain open issues until reproduced and tested.

Google source: https://android-developers.googleblog.com/2020/03/run-arm-apps-on-android-emulator.html
Emulator runner: https://github.com/ReactiveCircus/android-emulator-runner

