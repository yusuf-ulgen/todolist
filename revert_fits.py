import os
import re

files = [
    'activity_admin_feedback.xml', 'activity_change_password.xml',
    'activity_day_detail.xml', 'activity_giris.xml', 'activity_listelerim.xml', 'activity_main.xml',
    'activity_new_list.xml', 'activity_onboarding.xml', 'activity_reset_time.xml',
    'activity_settings.xml', 'activity_statistics.xml', 'activity_theme.xml'
]

for f in files:
    path = os.path.join('app/src/main/res/layout', f)
    if not os.path.exists(path):
        continue
    with open(path, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # Remove android:fitsSystemWindows="true" with any leading whitespace
    new_content = re.sub(r'\s*android:fitsSystemWindows="true"', '', content)
    
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as file:
            file.write(new_content)
        print(f"Removed fitsSystemWindows from {f}")
