# Word-inspired desktop refinement

This build keeps the Collabora engine and real editing commands. It adds smaller ribbon labels, horizontally scrollable tabs in narrow windows, a light-paper default and a persistent Light/Dark choice in the launcher. It does not clone Microsoft Word or imply identical features or document layout.

The primary acceptance device is Xiaomi Pad 7: user-reported 3200 × 2136 pixels, approximately 3:2. Styling uses density-aware CSS widths and a landscape aspect-ratio range rather than a fixed 3200-pixel canvas. The editor remains resizable and requests keyboard resize. No forced orientation or document-canvas scaling is introduced. Test full-screen landscape, portrait, half-screen and floating windows on this device; Android/HyperOS controls their available bounds. Physical-device behavior is not yet verified.

Fonts already supplied by Collabora remain. The cloud build adds Ubuntu's fonts-noto-core and fonts-dejavu-core, validates each font with fontconfig, and includes package versions, SHA-256 hashes and copyright/license files both inside the APK and alongside it. This is broader multilingual coverage, not every font. Proprietary fonts such as Aptos are not bundled without redistribution rights. CJK coverage still uses the upstream and Android fonts; fonts-noto-core is not the separate Noto CJK package.

Automated checks cover the UI asset integration, native font directory configuration, unchanged engine/DEX, Java compilation, APK signatures/alignment, font parsing and packaged font hashes. Physical-device validation is still required for actual font enumeration/rendering, mouse selection, context menus, shortcuts, touch, split-screen resizing and DOCX save/reopen fidelity. No claim that every editor feature has passed testing is made.

Tablet acceptance: update the existing app without uninstalling when the signing key matches; open a test copy of a DOCX; try light and dark; type accented Romanian, Arabic and Devanagari; select Noto/DejaVu fonts; save, fully close and reopen. Check bold/italic, paragraph formatting, tables, images, undo/redo, Ctrl+S and right-click with a physical mouse. Record the tablet model, Android version and any failing action before treating it as verified.

