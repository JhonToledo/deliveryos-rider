"""Genera el icono (1024x1024) y la pantalla de carga de la app iOS a partir del logo.

Uso (desde la raiz del proyecto):
    python scripts/gen_ios_assets.py ios/App/App/Assets.xcassets [ruta del logo]

Crea:
  - AppIcon.appiconset/AppIcon-512@2x.png: 1024x1024, SIN transparencia (la App Store la rechaza); iOS le pone las esquinas.
  - Splash.imageset/splash-2732x2732.png, -1.png y -2.png: la pantalla de carga (el storyboard la estira con scaleAspectFill,
    asi que el logo va en el centro y pequeno: en un telefono alto solo se ve el 46 % central del ancho).
Fondo: azul medianoche #1A202C (paleta oficial). Requiere Pillow.
"""
import os
import sys

from PIL import Image

LOGO_POR_DEFECTO = r"C:\Users\jairo\Documents\Proyectos Python\logos\logo_solo.png"
BG = (26, 32, 44)  # #1A202C

if len(sys.argv) < 2:
    sys.exit(__doc__)
assets = sys.argv[1]
logo = sys.argv[2] if len(sys.argv) > 2 else LOGO_POR_DEFECTO
src = Image.open(logo).convert("RGBA")
src = src.crop(src.getchannel("A").getbbox())  # recortar al contenido: se centra lo que se ve


def lienzo(lado, ancho_logo):
    im = Image.new("RGBA", (lado, lado), BG + (255,))
    h = round(src.height * ancho_logo / src.width)
    lg = src.resize((round(ancho_logo), h), Image.LANCZOS)
    im.alpha_composite(lg, ((lado - lg.width) // 2, (lado - lg.height) // 2))
    return im.convert("RGB")  # sin canal alfa


icono = os.path.join(assets, "AppIcon.appiconset")
carga = os.path.join(assets, "Splash.imageset")
os.makedirs(icono, exist_ok=True)
os.makedirs(carga, exist_ok=True)
lienzo(1024, 1024 * 0.64).save(os.path.join(icono, "AppIcon-512@2x.png"), optimize=True)
sp = lienzo(2732, 2732 * 0.30)
for nombre in ("splash-2732x2732.png", "splash-2732x2732-1.png", "splash-2732x2732-2.png"):
    sp.save(os.path.join(carga, nombre), optimize=True)
print("listo:", assets)
