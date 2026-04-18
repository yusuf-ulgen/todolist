import os
import re

files = [
    'activity_admin_feedback.xml', 'activity_change_password.xml',
    'activity_day_detail.xml', 'activity_giris.xml', 'activity_main.xml',
    'activity_new_list.xml', 'activity_onboarding.xml', 'activity_reset_time.xml',
    'activity_settings.xml', 'activity_theme.xml'
]

for f in files:
    path = os.path.join('app/src/main/res/layout', f)
    if not os.path.exists(path):
        continue
    with open(path, 'r', encoding='utf-8') as file:
        content = file.read()
    
    if 'android:fitsSystemWindows="true"' in content:
        continue
        
    content = re.sub(
        r'(xmlns:android=".*?"]*)',
        r'\1\n    android:fitsSystemWindows="true"',
        content,
        count=1
    )
    
    with open(path, 'w', encoding='utf-8') as file:
        file.write(content)
    print(f"Updated {f}")
