import os
import shutil

base_path = r"c:\Users\YUSUF\AndroidStudioProjects\todolist\app\src\main"
old_pkg = "com.example.todolist"
new_pkg = "com.yusufulgen.todolist"

# 1. Bulk replace in all files
for root, dirs, files in os.walk(base_path):
    for name in files:
        if name.endswith(('.kt', '.xml', 'google-services.json', '.kts')):
            path = os.path.join(root, name)
            try:
                with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                if old_pkg in content:
                    print(f"Updating {path}")
                    new_content = content.replace(old_pkg, new_pkg)
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
            except Exception as e:
                print(f"Error updating {path}: {e}")

# 2. Move directory
old_dir = os.path.join(base_path, "java", "com", "example", "todolist")
new_dir = os.path.join(base_path, "java", "com", "yusufulgen", "todolist")
if os.path.exists(old_dir):
    os.makedirs(os.path.dirname(new_dir), exist_ok=True)
    try:
        shutil.move(old_dir, new_dir)
        print(f"Moved {old_dir} to {new_dir}")
        # Cleanup
        shutil.rmtree(os.path.join(base_path, "java", "com", "example"))
    except Exception as e:
        print(f"Error moving directories: {e}")

# 3. Fix corrupted image (re-saving as clean PNG)
img_path = os.path.join(base_path, "res", "drawable", "onboard_task_feature.png")
if os.path.exists(img_path):
    print(f"Repairing image: {img_path}")
    try:
        from PIL import Image
        with Image.open(img_path) as img:
            img.save(img_path, "PNG")
        print("Image repaired successfully.")
    except Exception as e:
        print(f"Error repairing image (PIL might be missing): {e}")
        # Fallback: rewrite headers if PIL is missing, but simpler is best.

