"""Export the Android icon, outlined wordmark and web fonts. Requires fonttools + brotli.

Run from the repository root. PNG rasterization is separate (render these SVGs at
1024px for the icon and 2x for the wordmark in a browser with a transparent background).
"""
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parents[1]
HOSTED = ROOT / "docs/assets"
PUBLIC = ROOT / "public/docs"
HOSTED.mkdir(parents=True, exist_ok=True)
PUBLIC.mkdir(parents=True, exist_ok=True)
(HOSTED / "fonts").mkdir(exist_ok=True)
android = "{http://schemas.android.com/apk/res/android}"
icon = ET.parse(ROOT / "app/src/main/res/drawable/ic_singalong.xml").getroot()
paths = "".join(f'<path fill="{p.get(android + "fillColor")}" d="{p.get(android + "pathData")}"/>' for p in icon.findall("path"))
icon_svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 48 48" role="img" aria-label="singalong app icon">{paths}</svg>\n'
font_path = ROOT / "app/src/main/res/font/inter_variable.ttf"
font = TTFont(font_path)
axes = {a.axisTag for a in font["fvar"].axes}
font = instantiateVariableFont(font, {k: v for k, v in {"wght": 550, "opsz": 32}.items() if k in axes})
glyphs = font.getGlyphSet()
cmap = font.getBestCmap()
scale = 62 / font["head"].unitsPerEm
x = 112.0
letters = []
for char in "singalong":
    glyph = glyphs[cmap[ord(char)]]
    pen = SVGPathPen(glyphs)
    glyph.draw(TransformPen(pen, (scale, 0, 0, -scale, x, 76)))
    letters.append(f'<path d="{pen.getCommands()}"/>')
    x += glyph.width * scale - 1.6
width = round(x + 20)
logo_svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="{width * 2}" height="224" viewBox="0 0 {width} 112" role="img" aria-label="singalong"><g transform="translate(12 16) scale(1.667)">{paths}</g><g fill="#EDEBE4">{"".join(letters)}</g></svg>\n'
for folder in (HOSTED, PUBLIC):
    (folder / "singalong-icon.svg").write_text(icon_svg, encoding="utf-8")
    (folder / "singalong-logo.svg").write_text(logo_svg, encoding="utf-8")
for source, target in (("inter_variable.ttf", "inter.woff2"), ("pt_mono.ttf", "pt-mono.woff2")):
    webfont = TTFont(ROOT / "app/src/main/res/font" / source)
    webfont.flavor = "woff2"
    webfont.save(HOSTED / "fonts" / target)
for license_name in ("Inter-OFL.txt", "PTMono-OFL.txt"):
    shutil.copy2(ROOT / "licenses" / license_name, HOSTED / "fonts" / license_name)
print("Exported SVG icon/outlined wordmark to docs/assets and public/docs, plus hosted WOFF2 fonts.")
