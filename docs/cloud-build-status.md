# Writer cloud build

Date: 2026-10-11
Repository: https://github.com/mageorge808-rgb/desktop-office-writer

Successful hosted run: https://github.com/mageorge808-rgb/desktop-office-writer/actions/runs/38125968407
Download: https://github.com/mageorge808-rgb/desktop-office-writer/actions/runs/38125968407/artifacts/11697671298
Artifact size: 270,385,267 bytes (approximately 258 MiB). Retention: one day.
Both tests and APK jobs finished successfully. The signing key cache was saved successfully.

The active build uses the complete precompiled official ARM64 Collabora 26.04.3.1 APK,
whose SHA-256 was verified against the official distribution index. It retains the native
engine and original Java DEX. Two small Java classes supply a DOCX launcher and request
the native ribbon mode; the browser asset selects desktop form factor for Android.

The source revision associated with the embedded engine build id resolves to
20a46c332c380925803a1fe538a545c6f9b8fce7 in the official mirror; its tree includes both
engine and Android/Online code. Full per-component licenses and NOTICE are preserved.

Nine automated tests pass locally and on the hosted test runner. Hosted builds exposed
and fixed an obsolete Android SDK package request, an incorrect chained void API call,
and a Windows line ending appended during upload. Java compilation, D8, APK resource
packaging, signing, ZIP alignment, ARM64 payload checks and unchanged engine/DEX checks
have all passed on a GitHub-hosted Linux runner. The verified APK was uploaded successfully;
no tablet startup/editing/input acceptance test has been run.

No native build was attempted on the user's PC. Temporary native sources, archives and
binary inspection downloads were removed; only the lightweight source/fixtures remain.
The active workflow uses no self-hosted or paid runner. The latest Writer artifact is kept
for one day; previous Writer artifacts are removed before a successful replacement upload.

The development APK retains com.collabora.libreoffice, so an officially signed Collabora
installation cannot be updated with this differently signed package. Back up documents
and uninstall it before installing this development variant. The CI development key is
cached, but a cache reset may also require reinstalling future development APKs.

