# Attribution

## Active Writer cloud build

The active cloud build reuses the complete official ARM64 Collabora Office 26.04.3.1 APK.
Its URL/SHA-256 and embedded engine build id are recorded in `prebuilt.lock.json`.
The engine, original Java DEX, license.html and NOTICE are retained unchanged and checked.
`tests/fixtures/prebuilt-global.js` is the original browser asset from that binary,
used under the upstream MPL-2.0 terms. The modified asset and integration classes are
reproducible from `scripts/prebuilt.py` and `cloud/`.

Official binary origin: https://www.collaboraoffice.com/downloads/fdroid/repo/
Official source origin: https://gerrit.collaboraoffice.com/online
Read-only mirror: https://github.com/CollaboraOnline/online.mirror
Engine build id embedded in the original: 20a46c332c38.
The earlier main-branch revision below describes the initial source-port experiment,
not the source revision of this stable precompiled release. Do not conflate them.

The APK artifact contains the original complete license/NOTICE documents and the
modified-source archive. All upstream trademarks identify the reused implementation;
Desktop Office Writer is an independent development customization.

## Earlier source integration / regression fixtures

Collabora Android integration is reused from the official read-only mirror:
https://github.com/CollaboraOnline/online.mirror
Revision: a79484ec9b6bed718695e373ac517365012bd4d5.

`tests/fixtures/upstream-global.js` and `tests/fixtures/upstream-LOActivity.java`
are exact source snapshots from that revision, retained to test the overlay against
actual upstream code without downloading the native engine in every test run.
They remain subject to the upstream Mozilla Public License 2.0.

The editor and engine are fetched at build time, not replaced by our launcher.
Their headers, license files and generated third-party notices remain intact.
See `licenses/` and the engine's generated `instdir/LICENSE.html` and `instdir/NOTICE`.
The copied LibreOffice COPYING file is one upstream license text, not a statement
that every engine file uses that license; preserve the per-file and bundled-library terms.

Original project source changes are MPL-2.0. The launcher icon is original.
Desktop Office is an independent integration, not an official Collabora or Microsoft product.

