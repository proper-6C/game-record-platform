import os, subprocess, re

skill = r"C:\Users\27754\AppData\Local\Doubao\User Data\Default\.doubao\agent_mode\workspace\.skills\html\scripts\shot.py"
py = r"C:\Users\27754\AppData\Local\Doubao\User Data\sandbox_runtime\bases\c98c5042338ed152c6f10ecd8591889f\python\python.exe"
path = r"D:\soft\code\Save_code\First-db\home-preview\主页方案B预览.html"
r = subprocess.run([py, skill, path], capture_output=True, text=True, encoding="utf-8", errors="replace")
out = r.stdout or ""
for key in ["consoleErrors", "resourceErrors", "horizontalOverflow"]:
    m = re.search(key + r'\s*:\s*\[(.*?)\]', out, re.S)
    print(key, "=>", (m.group(1).strip()[:150] if m else "?"))
m = re.search(r'"firstH1": "(.*?)"', out)
print("h1 =>", m.group(1) if m else "?")
print("exit:", r.returncode)
print("shots:", sorted(os.listdir(os.path.join(os.path.dirname(path), "_shots"))))
