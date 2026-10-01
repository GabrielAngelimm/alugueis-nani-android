"""Render launcher fallbacks and the review sheet. Requires pillow and resvg-py."""
from pathlib import Path
from io import BytesIO
import resvg_py
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
BRAND = ROOT / 'brand'
paths = [('7', 'M29,47 L54,27 L79,47'), ('8', 'M37,73 V48 L71,73 V48')]
gradient = '<linearGradient id="ink" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#6870D9"/><stop offset="0.55" stop-color="#4851AF"/><stop offset="1" stop-color="#303771"/></linearGradient>'
mark = ''.join(f'<path fill="none" stroke="#F7F3EC" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round" d="{d}"/>' for w, d in paths)
svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><defs>{gradient}</defs><rect width="108" height="108" rx="24" fill="url(#ink)"/>{mark}</svg>'
(BRAND / 'nani-symbol.svg').write_text(svg + '\n', encoding='utf-8')
foreground = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
foreground += '\n'.join(f'    <path android:fillColor="@android:color/transparent" android:strokeColor="#F7F3EC" android:strokeWidth="{w}" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}" />' for w, d in paths)
foreground += '\n</vector>\n'
(RES / 'drawable/ic_launcher_foreground.xml').write_text(foreground, encoding='utf-8')
(RES / 'drawable/ic_launcher_background.xml').write_text('''<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0H108V108H0Z">
        <aapt:attr name="android:fillColor">
            <gradient android:startX="0" android:startY="0" android:endX="108" android:endY="108" android:type="linear">
                <item android:offset="0" android:color="#6870D9" />
                <item android:offset="0.55" android:color="#4851AF" />
                <item android:offset="1" android:color="#303771" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''', encoding='utf-8')

def render(source, size):
    return Image.open(BytesIO(resvg_py.svg_to_bytes(svg_string=source, width=size, height=size))).convert('RGBA')

icon = render(svg, 512)
icon.save(BRAND / 'nani-icon.png')
for density, size in [('mdpi',48), ('hdpi',72), ('xhdpi',96), ('xxhdpi',144), ('xxxhdpi',192)]:
    render(svg, size).save(RES / f'mipmap-{density}/ic_launcher.webp', lossless=True)
    circle = svg.replace('<rect width="108" height="108" rx="24" fill="url(#ink)"/>', '<circle cx="54" cy="54" r="54" fill="url(#ink)"/>')
    render(circle, size).save(RES / f'mipmap-{density}/ic_launcher_round.webp', lossless=True)

board = Image.new('RGB', (1000, 630), '#F1F0ED')
draw = ImageDraw.Draw(board)
draw.text((48,32), 'ALUGUEIS NANI / CASA + N', fill='#303771', font_size=24)
board.paste(icon.resize((330,330)), (48,105), icon.resize((330,330)))
# Android masks expose the central 72dp of the 108dp adaptive layer.
adaptive = svg.replace('viewBox="0 0 108 108"', 'viewBox="18 18 72 72"').replace('rx="24"', 'rx="0"')
for x, label, round_mask in [(450,'Adaptativo',False),(720,'Circular',True)]:
    tile = render(adaptive, 210)
    mask = Image.new('L',(210,210))
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse((0,0,209,209),fill=255)
    else:
        md.rounded_rectangle((0,0,209,209),radius=48,fill=255)
    board.paste(tile,(x,130),mask)
    draw.text((x,365),label,fill='#303771',font_size=20)
for i, size in enumerate([48,32,24]):
    small = render(svg,size)
    board.paste(small,(450+i*90,440),small)
    draw.text((450+i*90,500),f'{size}px',fill='#303771',font_size=16)
mono = render(svg.replace('url(#ink)','#DEE0F5').replace('#F7F3EC','#303771'),96)
board.paste(mono,(775,420),mono)
draw.text((735,540),'Tema monocromatico',fill='#303771',font_size=18)
board.save(BRAND / 'nani-icon-preview.png')
