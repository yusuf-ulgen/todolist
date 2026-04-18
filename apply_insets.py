import os

root_dir = 'app/src/main/java/com/example/todolist/ui/view'

for file_name in os.listdir(root_dir):
    if not file_name.endswith('.kt'):
        continue
    
    path = os.path.join(root_dir, file_name)
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'WindowInsetsHelper.applyTopBottomInsets(binding.root)' in content:
        continue
    
    # Let's find "setContentView(binding.root)"
    target = 'setContentView(binding.root)'
    if target in content:
        # replace the first occurrence
        content = content.replace(
            target,
            target + '\n        com.example.todolist.WindowInsetsHelper.applyTopBottomInsets(binding.root)',
            1
        )
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Applied insets to {file_name}")
