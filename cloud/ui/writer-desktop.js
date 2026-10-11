/* SPDX-License-Identifier: MPL-2.0 */
// Load after the original styles. Keep native input, canvas coordinates and UNO commands intact.
window.addEventListener('load', function () {
    if (!window.ThisIsTheAndroidApp) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'writer-desktop.css';
    document.head.appendChild(link);
});

