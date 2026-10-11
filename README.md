# Desktop Office Writer — free cloud APK build

**Press Actions → Build Writer APK → Run workflow.** Download
`desktop-office-writer-arm64` from the finished run's Artifacts section, unzip it,
and install `Desktop-Office-Writer-arm64.apk` on your ARM64 Android tablet (Android 8+).

This is a real offline DOCX editor using the official precompiled Collabora Office
26.04.3.1 engine, Java implementation and document resources as a matched set.
The workflow compiles only two small integration classes and packages the APK.
It never compiles LibreOffice, uses no self-hosted runner, and requires no paid service.
Use a **public GitHub repository** for free standard GitHub-hosted runner execution.

## Build and download

1. Open this repository's **Actions** tab. Enable workflows if GitHub asks.
2. Select **Build Writer APK** on the left, then **Run workflow**, branch **main**.
3. Open the new run. Tests run first, followed by APK packaging on `ubuntu-24.04`.
4. When both jobs are green, scroll to **Artifacts** and download
   **desktop-office-writer-arm64**. You must be signed into GitHub.
5. Unzip the download and transfer the APK to your tablet. Allow installation from
   that browser/file manager when Android asks.

Pushes to main that change the build also generate an APK automatically.
Only the latest Writer artifact is retained, for **one day**, to keep artifact storage
below the free allowance. Download it promptly; rerun the workflow if it expires.
The archive includes checksums, license/NOTICE files, signature/alignment reports,
binary provenance and all modified source. APK size is approximately 255 MiB plus
small integration changes; final size is measured by the build.

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

- DOCX-only launcher: **Open DOCX**, **New DOCX**, Ctrl+O and Ctrl+N.
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

These checks do **not** establish document fidelity, startup on every Android version,
crash recovery or physical mouse/keyboard behavior. Follow `docs/device-test-plan.md`
on real hardware before treating the editor as stable. Local tests need only Python
and Node; all Java/APK packaging is performed in the cloud.

## Licenses and source

Our changes are MPL-2.0. Original binaries retain their own licenses and the entire
embedded license/NOTICE payload. The build emits a modified-source archive and exact
upstream APK URL/checksum/build id. Source origins and reproduction instructions are
in `NOTICE.md`. This is an independent modified integration, not an official Collabora
or Microsoft product. No Microsoft assets are used.

Official distribution: https://www.collaboraoffice.com/downloads/fdroid/repo/
Official source/build documentation: https://github.com/CollaboraOnline/online.mirror
GitHub free runners: https://docs.github.com/en/actions/reference/runners/github-hosted-runners

