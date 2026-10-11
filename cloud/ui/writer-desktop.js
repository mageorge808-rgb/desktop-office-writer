/* SPDX-License-Identifier: MPL-2.0 */
// Load after the original styles. Keep native input, canvas coordinates and UNO commands intact.
window.addEventListener('load', function () {
    if (!window.ThisIsTheAndroidApp) return;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'writer-desktop.css';
    document.head.appendChild(link);
});

// Direct access to the real table-properties dialog; do not consume typing or
// edit shortcuts while a dialog is active or the document is read-only.
window.addEventListener('keydown', function (event) {
    if (!window.ThisIsTheAndroidApp || !event.ctrlKey || !event.altKey || event.shiftKey ||
            event.code !== 'KeyT' || event.repeat || event.isComposing ||
            (typeof event.getModifierState === 'function' && event.getModifierState('AltGraph'))) return;
    const map = window.app && window.app.map;
    if (!map || !map.isEditMode() || !map.stateChangeHandler ||
            map.stateChangeHandler.getItemValue('.uno:TableDialog') !== 'enabled' ||
            document.querySelector('.jsdialog-window:not(.modalpopup), .lokdialog_container')) return;
    event.preventDefault();
    event.stopPropagation();
    map.sendUnoCommand('.uno:TableDialog');
}, true);

