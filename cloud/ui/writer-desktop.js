/* SPDX-License-Identifier: MPL-2.0 */
// Load after the original styles. Keep native input, canvas coordinates and UNO commands intact.
window.addEventListener('load', function () {
    if (!window.ThisIsTheAndroidApp) return;
    // The upstream Styles gallery cannot fold, so it pushes Font/Paragraph into
    // menus even on a wide tablet. Let the existing overflow manager fold that
    // gallery first; its original controls and responsive resize logic remain.
    const writer = window.L && window.L.Control && window.L.Control.NotebookbarWriter;
    if (writer && typeof writer.prototype.getHomeTab === 'function') {
        const originalHome = writer.prototype.getHomeTab;
        writer.prototype.getHomeTab = function () {
            const tab = originalHome.apply(this, arguments);
            function visit(item) {
                if (item.id === 'home-stylesview-group') item.nofold = false;
                if (Array.isArray(item.children)) item.children.forEach(visit);
            }
            visit(tab);
            return tab;
        };
    }
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'writer-desktop.css';
    document.head.appendChild(link);
});

// Direct access to the real table-properties dialog; do not consume typing or
// edit shortcuts while a dialog is active or the document is read-only.
window.addEventListener('keydown', function (event) {
    if (!window.ThisIsTheAndroidApp || !event.ctrlKey || !event.altKey || event.shiftKey ||
            (event.code !== 'KeyT' && String(event.key).toLowerCase() !== 't') || event.repeat || event.isComposing ||
            (typeof event.getModifierState === 'function' && event.getModifierState('AltGraph'))) return;
    const map = window.app && window.app.map;
    if (!map || !map.isEditMode() || !map.stateChangeHandler ||
            map.stateChangeHandler.getItemValue('.uno:TableDialog') !== 'enabled' ||
            document.querySelector('.jsdialog-window:not(.modalpopup), .lokdialog_container')) return;
    event.preventDefault();
    event.stopPropagation();
    map.sendUnoCommand('.uno:TableDialog');
}, true);

