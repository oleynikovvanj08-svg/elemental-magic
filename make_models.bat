@echo off
chcp 65001 >nul
setlocal

set BASE=src\main\resources\assets\elementalmagic\models\item
if not exist "%BASE%" mkdir "%BASE%"

for %%E in (lightning light darkness water earth air fire) do (
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/book"}} > "%BASE%\%%E_spellbook.json"
    echo {"parent":"minecraft:item/handheld","textures":{"layer0":"minecraft:item/stick"}} > "%BASE%\%%E_staff.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/amethyst_shard"}} > "%BASE%\%%E_amulet.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/gold_ingot"}} > "%BASE%\%%E_ring.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/iron_helmet"}} > "%BASE%\%%E_helmet.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/iron_chestplate"}} > "%BASE%\%%E_chestplate.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/iron_leggings"}} > "%BASE%\%%E_leggings.json"
    echo {"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/iron_boots"}} > "%BASE%\%%E_boots.json"
)

echo Готово! Создано 56 файлов моделей.
pause