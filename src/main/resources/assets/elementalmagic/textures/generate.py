from PIL import Image
import os

# Цвета стихий в формате RGB (красный, зелёный, синий)
colors = {
    "lightning": (127, 219, 255),
    "light": (255, 243, 160),
    "darkness": (138, 43, 226),
    "water": (30, 144, 255),
    "earth": (139, 90, 43),
    "air": (200, 247, 247),
    "fire": (255, 69, 0),
    "nature": (60, 176, 67),
    "ash": (158, 158, 158),
    "explosion": (255, 140, 0),
    "elemental": (64, 224, 208),
    "time": (212, 175, 55),
    "stars": (176, 196, 255),
    "dark_nature": (85, 107, 47),
    "shadow": (75, 75, 110),
}

# Создаём папку item, если её нет
os.makedirs("item", exist_ok=True)

# Для каждой стихии создаём квадратики 16×16
for element, color in colors.items():
    img = Image.new("RGB", (16, 16), color)
    
    # Книги, посохи, аксессуары
    img.save(f"item/{element}_spellbook.png")
    img.save(f"item/{element}_staff.png")
    img.save(f"item/{element}_amulet.png")
    img.save(f"item/{element}_ring.png")
    
    # Броня: шлем, нагрудник, поножи, ботинки
    img.save(f"item/{element}_helmet.png")
    img.save(f"item/{element}_chestplate.png")
    img.save(f"item/{element}_leggings.png")
    img.save(f"item/{element}_boots.png")
    
    # Свитки 5 уровней
    for tier in range(1, 6):
        img.save(f"item/{element}_scroll_{tier}.png")

# Магический стол — фиолетовый
purple = Image.new("RGB", (16, 16), (138, 43, 226))
purple.save("item/magic_table.png")

print("✓ Создано ~200 PNG-файлов в папке item/")
