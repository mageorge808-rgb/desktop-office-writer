# Writer cloud build

Verified: 2026-10-11
Repository: https://github.com/mageorge808-rgb/desktop-office-writer
Source commit: fdeac1da969d073a7ca48493435eb4d548b4ebf8
Successful run: https://github.com/mageorge808-rgb/desktop-office-writer/actions/runs/38133351166
Release: https://github.com/mageorge808-rgb/desktop-office-writer/releases/tag/writer-build-15-1
Direct APK: https://github.com/mageorge808-rgb/desktop-office-writer/releases/latest/download/Desktop-Office-Writer-arm64.apk

The direct APK download returned HTTP 200 with the Android APK content type.
The APK is 287,995,928 bytes. GitHub reports SHA-256
1f9ff5ef54809103b93678a3e7bd6f04db35c18336ec4540e3ac51602ae52639.
The release includes source, font licenses, upstream provenance, checksums and
signing/alignment reports. Release assets are independent of the one-day Actions
artifact retention. New tested main-branch builds publish a new release automatically.

All 14 unit/browser tests passed. Java compilation, D8, APK resource packaging,
signing, ZIP alignment, ARM64 checks and unchanged original engine/DEX checks passed.
The shipping ARM64 APK was installed and run through the Android 15 Google APIs
emulator's ARM translation bridge on a standard GitHub-hosted Ubuntu runner.

All four native phases passed: editable DOCX open, insertion of text and a 3-by-2
table, local URI save-back and XML verification; force-stop/reopen with rendered
page tiles; Android keyboard typing with persisted bold via Ctrl+B, save via Ctrl+S,
and the real Table Properties dialog via Ctrl+Alt+T; then opening with read-only
URI permissions. The real engine enumerated Noto Sans and DejaVu Sans. Full-width
Home Font/Paragraph controls were visible and the original Styles gallery received
live entries. Tests use a disposable document and a separate test-only provider APK.

Measured load times on this cloud emulator were 15,970 ms initially, 9,577 ms on
reopen, 11,248 ms for keyboard testing, and 10,720 ms in read-only mode. These are
cloud-run measurements, not Pad 7 benchmarks or a demonstrated speed improvement.
The font-cache test verifies no repeated font copying on a warm document open.

The official precompiled Collabora 26.04.3.1 engine, original Java DEX, document
resources and license payload are retained as a matched set. Engine source revision
20a46c332c380925803a1fe538a545c6f9b8fce7 includes the engine and Android/Online sources.
No full native engine build or Android toolchain download was attempted on the PC.
No paid service or self-hosted runner is used.

Known limitations: black DOCX table borders remain difficult to see on a dark page;
the improved insertion picker does not fix native document-tile rendering. Physical
Pad 7 mouse selection, right-click, scrolling, HyperOS resizing and full document
fidelity still require device acceptance. Android 15 passed; the manifest's older
supported Android versions have not all passed runtime tests. This is a development
build, not certification of every Microsoft Word feature.

The development APK retains com.collabora.libreoffice. It can update an earlier
Writer build when the cached development signing key matches, but cannot update
officially signed Collabora. Back up local documents before any required uninstall.
An expired signing cache may require reinstalling a future development build.
