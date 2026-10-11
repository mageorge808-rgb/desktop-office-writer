/* Collabora Online contributors; SPDX-License-Identifier: MPL-2.0
 * Fixture from online.mirror 20a46c332c380925803a1fe538a545c6f9b8fce7.
 * Focus handler is stubbed; tests cover DOM geometry, not native commands. */
const highlightTableFunction=function() {};
function insertTable(grid = document.getElementsByClassName('inserttable-grid')[0]) {
	var rows = 10;
	var cols = 10;

	for (var r = 0; r < rows; r++) {
		const row = document.createElement('div');
		row.className = 'row';
		row.setAttribute('role', 'row');
		grid.appendChild(row);

		for (var c = 0; c < cols; c++) {
			const col = document.createElement('button');
			col.setAttribute('role', 'gridcell');
			col.setAttribute('aria-label', (1 + r) + 'x' + (1 + c));
			col.onfocus = highlightTableFunction;
			col.className = 'col';
			row.appendChild(col);
		}
	}
}

