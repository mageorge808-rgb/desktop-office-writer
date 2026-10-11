# Desktop Office Writer — free cloud APK build

**[Download the latest tested Writer APK](https://github.com/mageorge808-rgb/desktop-office-writer/releases/latest/download/Desktop-Office-Writer-arm64.apk).**
Open the downloaded file on your ARM64 Android tablet and allow installation from
your browser/file manager if Android asks. No ZIP extraction is required.

Source, licenses, checksums and build notes accompany
[the latest development release](https://github.com/mageorge808-rgb/desktop-office-writer/releases/latest).
Release downloads do not use the one-day Actions artifact retention.

This is a real offline DOCX editor using the official precompiled Collabora Office
26.04.3.1 engine, Java implementation and document resources as a matched set.
The workflow compiles a small Java integration layer and packages the APK.
It never compiles LibreOffice, uses no self-hosted runner, and requires no paid service.
Use a **public GitHub repository** for free standard GitHub-hosted runner execution.

Builds and test status: https://github.com/mageorge808-rgb/desktop-office-writer/actions
The workflow runs unit/browser tests, Java/APK checks and native Android smoke tests.
Physical Pad 7 mouse/keyboard acceptance remains required; this is not Microsoft Word.

## Build and download

1. Open this repository's **Actions** tab. Enable workflows if GitHub asks.
2. Select **Build Writer APK** on the left, then **Run workflow**, branch **main**.
3. Open the new run. Tests run first, followed by APK packaging and Android emulator checks on `ubuntu-24.04`.
4. When both jobs are green, open **Releases → Latest** and download
   **Desktop-Office-Writer-arm64.apk** directly. Main-branch builds publish this automatically.
5. Open the APK on your tablet. Allow installation from that browser/file manager
   when Android asks. The Actions artifact is an alternative ZIP download for signed-in users.

Pushes to main that change the build also generate an APK automatically.
Only the latest Actions Writer artifact is retained, for **one day**, to keep artifact storage
below the free allowance. The tested APK in Releases remains available after that artifact expires.
The archive includes checksums, license/NOTICE files, signature/alignment reports,
binary provenance and all modified source. The verified build of 2026-10-11 is
287,995,928 bytes (approximately 275 MiB); future build sizes are measured by CI.

## Installation identity and signing

The development APK intentionally retains package id **com.collabora.libreoffice**,
which the matched upstream integration and font paths expect. It is signed with our
development key, not Collabora's key. **Back up documents and uninstall official
Collabora Office before installing this build** if that app is already installed.
It cannot be installed beside official Collabora under a different name yet.
Uninstalling an app deletes its private settings/cache; save documents to local storage first.

The workflow caches its development signing key between builds. If that cache expires,
the next build gets a new key and Android may require an uninstall before updating.
This is a development distribution, not a production signing setup.

## Implemented source changes

- DOCX-only launcher: **Open DOCX**, **New DOCX**, Ctrl+O and Ctrl+N, also available while editing.
- Ctrl+Alt+T requests the native Table Properties dialog when its command is enabled.
- Dark mode on install/upgrade; persistent appearance choice and dark launcher controls.
- A compact Home Styles gallery gives Font and Paragraph controls room on wide windows.
- Cached, licensed Noto/DejaVu fonts; no repeated copying on every document open.
- Android file picker and actual read/write grants; no broad all-files permission.
- New documents copy the real upstream DOCX template into the selected local URI.
- Real editing/save-back/export use the existing Collabora engine and Activity.
- A subclass requests native `notebookbar` mode on every screen size.
- Guarded browser changes select desktop form factor and notebookbar on Android,
  preserving the Android socket/clipboard/native bridge.
- Original native libraries and original DEX are checked byte-for-byte after packaging.
  The compile-only Activity stub must never be included in the APK.
- Existing desktop mouse selection, context menus, scrolling and editing shortcuts are
  reused. **These still need physical-tablet acceptance tests.**

This milestone focuses on Writer. The matched binary still contains Calc/Impress code
because deleting individual engine pieces is not safe, but those are not exposed by the
launcher or advertised as implemented products.

## What the automated checks prove

The upstream APK and packaging tool are SHA-256 pinned in `prebuilt.lock.json`.
Tests execute the actual upstream form-factor logic and verify DOCX-only manifest
exposure. CI compiles the Java classes, repackages resources, checks the native JNI
signature, verifies ARM64 ELF payloads, ZIP alignment and APK signatures, and proves
the original engine/DEX/licenses are unchanged.

Native smoke checks exercise a disposable DOCX through a test-only local content
provider, including save and reopen. Native keyboard events also check typing,
Ctrl+B formatting, Ctrl+S save-back and Ctrl+Alt+T table properties. They do **not** establish full document fidelity,
startup on every Android version, crash recovery or physical mouse/keyboard behavior. Follow `docs/device-test-plan.md`
on real hardware before treating the editor as stable. Local tests need only Python
and Node; all Java/APK packaging is performed in the cloud.

Run [38133351166](https://github.com/mageorge808-rgb/desktop-office-writer/actions/runs/38133351166)
passed all 14 unit/browser tests and the four native phases (edit/save, reopen,
keyboard, read-only) with the shipping ARM64 APK on Android 15. Native checks also
confirm rendered page tiles, Noto Sans/DejaVu Sans in the engine's font list, and
visible Home Font/Paragraph controls with a populated Styles gallery. These checks
do not measure speed or input behavior on a physical Pad 7.

Known issue: actual black table borders still have poor contrast against a dark
document page. The insertion picker is improved, and Ctrl+Alt+T opens the real Table
Properties dialog; the document-border rendering issue remains open.

## Licenses and source

Our changes are MPL-2.0. Original binaries retain their own licenses and the entire
embedded license/NOTICE payload. The build emits a modified-source archive and exact
upstream APK URL/checksum/build id. Source origins and reproduction instructions are
in `NOTICE.md`. This is an independent modified integration, not an official Collabora
or Microsoft product. No Microsoft assets are used.

Official distribution: https://www.collaboraoffice.com/downloads/fdroid/repo/
Official source/build documentation: https://github.com/CollaboraOnline/online.mirror
Matching precompiled release source: https://github.com/CollaboraOnline/online.mirror/tree/20a46c332c380925803a1fe538a545c6f9b8fce7
GitHub free runners: https://docs.github.com/en/actions/reference/runners/github-hosted-runners
