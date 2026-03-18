import os

base_dir = r"c:\Users\YUSUF\AndroidStudioProjects\todolist\app\src\main\res"
folders = ["mipmap-hdpi", "mipmap-mdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"]

for folder in folders:
    old_path = os.path.join(base_dir, folder, "icon.png")
    new_path = os.path.join(base_dir, folder, "icon_foreground.png")
    if os.path.exists(old_path):
        try:
            os.rename(old_path, new_path)
            print(f"Renamed in {folder}")
        except Exception as e:
            print(f"Error in {folder}: {e}")
    else:
        print(f"File not found in {folder}")

# Delete the problematic PNG in anydpi-v26
anydpi_png = os.path.join(base_dir, "mipmap-anydpi-v26", "icon.png")
if os.path.exists(anydpi_png):
    try:
        os.remove(anydpi_png)
        print("Deleted icon.png in anydpi-v26")
    except Exception as e:
        print(f"Error deleting anydpi png: {e}")

print("Done")
