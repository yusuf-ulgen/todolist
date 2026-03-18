@echo off
ren app\src\main\res\mipmap-hdpi\icon.png icon_foreground.png
ren app\src\main\res\mipmap-mdpi\icon.png icon_foreground.png
ren app\src\main\res\mipmap-xhdpi\icon.png icon_foreground.png
ren app\src\main\res\mipmap-xxhdpi\icon.png icon_foreground.png
ren app\src\main\res\mipmap-xxxhdpi\icon.png icon_foreground.png
del app\src\main\res\mipmap-anydpi-v26\icon.png
echo Done
