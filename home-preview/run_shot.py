import os, subprocess, sys

skill = r"C:\Users\27754\AppData\Local\Doubao\User Data\Default\.doubao\agent_mode\workspace\.skills\html\scripts\shot.py"
html_dir = r"D:\soft\code\Save_code\First-db\home-preview"
target = [f for f in os.listdir(html_dir) if f.endswith(".html")][0]
html_path = os.path.join(html_dir, target)
py = r"C:\Users\27754\AppData\Local\Doubao\User Data\sandbox_runtime\bases\c98c5042338ed152c6f10ecd8591889f\python\python.exe"
print("target:", target)
r = subprocess.run([py, skill, html_path], capture_output=True, text=True, encoding="utf-8", errors="replace")
print(r.stdout[-4000:] if r.stdout else "")
print("STDERR:", r.stderr[-1500:] if r.stderr else "")
print("exit:", r.returncode)
