# Dark mode, table controls and font-cache fixes

The requested default is dark on new installations and existing upgrades. A migration flag applies it once; a subsequent appearance choice is preserved. The launcher also has a dark background and light text.

The added fonts previously copied on every open. FontInstaller now uses a content-derived index, a completion marker and file-size checks. Warm starts open no font/config payload streams. Cold installs and updates use atomic replacements, reject malformed paths and truncated assets, and commit the marker only after success. Missing files trigger repair. It does not speed up the native DOCX parser, guarantee a frame rate or detect same-size external corruption on every open.

The Insert Table grid now has consistent 25px targets and bright borders in dark mode. Dialog borders have higher contrast. Global font-size overrides were removed because they also affected dialogs beyond the ribbon. Browser tests cover contrast, non-overlapping hit targets and click targeting at full-screen, split-screen, narrow and portrait CSS sizes for the Pad 7. Native Android input and actual DOCX table rendering still require device testing.

Actual document borders are rendered by Collabora's precompiled native engine. CSS cannot repair black table border rendering in document tiles. This build does not change saved document border colors or claim that this native issue is fixed. The next useful evidence is a screenshot and a small DOCX reproducing the problem, especially whether it affects new tables, existing tables, printed borders or non-printing guides. See the engine's table border controls in the native Table Properties dialog; changing a border color there changes the document.

Tests execute the real Java installer for warm-start zero-payload reads, updates, missing-file recovery, failed copies and path rejection; real Chromium checks the original picker structure/styles with the overlay. APK checks still validate the signature, alignment, native architecture, font inventory and preserved engine/DEX.

