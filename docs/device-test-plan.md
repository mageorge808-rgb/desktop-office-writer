# Writer acceptance tests — pending execution

Record APK SHA-256, device model, Android/WebView versions, keyboard/mouse models and
upstream revision. Keep original files and compare saved copies, never overwrite fixtures.

1. Install the generated ARM64 APK. Disable Wi-Fi/mobile data. Open an existing DOCX
   through Android's local file picker. Confirm the desktop ribbon is visible in landscape,
   portrait, split screen and external-display modes.
2. Type a unique marker, apply bold and a paragraph style. Save with Ctrl+S. Close and
   reopen via the picker. Confirm the marker and formatting survived. Open the saved copy
   in desktop LibreOffice and Word if available; record differences.
3. Repeat with lists, tables, embedded images, headers/footers and page breaks. Compare
   document text, object counts and rendered pages. Count unexpected losses as failures.
4. Select a word by double-click and a precise text range by dragging. Extend with Shift
   and Ctrl+Shift+arrows. Right-click inside/outside selection: the editor menu must open
   at the pointer; copy/paste must act on the correct range. Check wheel and Shift+wheel.
5. Verify Ctrl+A/C/X/V/Z/Y/S/F, arrows, Home/End, Ctrl+Home/End, Tab, Enter and Delete.
   Test accents/diacritics and keyboard attach/detach while editing. Check clipboard with
   another Android application and ensure shortcut events do not execute twice.
6. Open from a provider that grants read but not write. Confirm no silent overwrite or
   false success. Check Save As DOCX, cancel, insufficient space and revoked permissions.
7. Edit, rotate, background, then reopen. Force-stop the process and assess actual recovery.
   Do not describe upstream pause-save as guaranteed crash recovery.
8. Open a long document and measure load time, typing latency, memory and scrolling.

Acceptance requires steps 1–6 passing on physical hardware and no unexpected document
loss in the agreed fixture corpus. Steps 7–8 establish recovery/performance limits.
Attach saved DOCX files, comparison results, screenshots and logs to the build record.

