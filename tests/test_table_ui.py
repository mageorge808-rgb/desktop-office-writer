"""Browser-level checks of actual upstream picker DOM/CSS plus our overlay, not native DOCX rendering."""
import os
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

def luminance(rgb):
    values = [v / 255 for v in rgb]
    values = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in values]
    return sum(a*b for a,b in zip(values, [.2126,.7152,.0722]))

class TableUITests(unittest.TestCase):
    def test_dark_picker_contrast_layout_and_click_targets(self):
        try:
            from playwright.sync_api import sync_playwright
        except ImportError:
            if os.environ.get('CI'): self.fail('CI must install the browser test dependency')
            self.skipTest('Browser tests execute in cloud to avoid local downloads')
        fixture = ROOT/'tests/fixtures/table-picker'
        native_css = (fixture/'upstream.css').read_text()
        overlay = (ROOT/'cloud/ui/writer-desktop.css').read_text()
        dom = (fixture/'upstream-insert-table.js').read_text()
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch()
            for width,height in [(1280,854), (800,534), (400,534), (854,1280)]:
                with self.subTest(viewport=(width,height)):
                    page = browser.new_page(viewport={'width':width,'height':height})
                    page.set_content('<html data-theme="dark"><style>body{margin:8px;background:#121212;}'+native_css+overlay+'</style><div class="inserttable-grid" role="grid"></div></html>')
                    page.add_script_tag(content=dom+'\ninsertTable();')
                    self.assertEqual(page.locator('.inserttable-grid .col').count(),100)
                    metrics = page.locator('.inserttable-grid .col').first.evaluate('''el => {
                        const css=getComputedStyle(el), rect=el.getBoundingClientRect();
                        return {border:css.borderColor.match(/\\d+/g).map(Number),
                            background:css.backgroundColor.match(/\\d+/g).map(Number),
                            width:rect.width,height:rect.height};
                    }''')
                    a,b = luminance(metrics['border']),luminance(metrics['background'])
                    self.assertGreaterEqual((max(a,b)+.05)/(min(a,b)+.05),3)
                    self.assertGreaterEqual(metrics['width'],24)
                    self.assertGreaterEqual(metrics['height'],24)
                    boxes = page.locator('.inserttable-grid .col').evaluate_all('els => els.map(el=>{const r=el.getBoundingClientRect();return [r.x,r.y,r.width,r.height];})')
                    for index,(x,y,w,h) in enumerate(boxes):
                        self.assertLessEqual(x+w,width)
                        if index%10: self.assertGreaterEqual(x,boxes[index-1][0]+boxes[index-1][2])
                        if index>=10: self.assertGreaterEqual(y,boxes[index-10][1]+boxes[index-10][3])
                    page.evaluate('''() => { window.picked=null; document.querySelector('.inserttable-grid').addEventListener('click', e=>window.picked=e.target.getAttribute('aria-label')); }''')
                    page.locator('[aria-label="3x4"]').click()
                    self.assertEqual(page.evaluate('window.picked'),'3x4')
                    output=ROOT/'build/ui'; output.mkdir(parents=True,exist_ok=True)
                    page.screenshot(path=str(output/f'dark-table-{width}x{height}.png'))
                    page.close()
            browser.close()

