import os, subprocess, json, re

skill = r"C:\Users\27754\AppData\Local\Doubao\User Data\Default\.doubao\agent_mode\workspace\.skills\html\scripts\shot.py"
html_dir = r"D:\soft\code\Save_code\First-db\home-preview"
py = r"C:\Users\27754\AppData\Local\Doubao\User Data\sandbox_runtime\bases\c98c5042338ed152c6f10ecd8591889f\python\python.exe"

targets = ["主页方案B预览.html", "主页方案C预览.html", "主页方案D预览.html"]
for t in targets:
    path = os.path.join(html_dir, t)
    r = subprocess.run([py, skill, path], capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = r.stdout or ""
    # 提取关键字段
    m = re.search(r'consoleErrors.*?\[(.*?)\]', out, re.S)
    cerr = m.group(1).strip() if m else "?"
    m2 = re.search(r'resourceErrors.*?\[(.*?)\]', out, re.S)
    rerr = m2.group(1).strip() if m2 else "?"
    m3 = re.search(r'"firstH1": "(.*?)"', out)
    h1 = m3.group(1) if m3 else "?"
    m4 = re.search(r'horizontalOverflow.*?\[(.*?)\]', out, re.S)
    hov = m4.group(1).strip() if m4 else "?"
    print("===", t)
    print("exit:", r.returncode, "| consoleErrors:", cerr[:120], "| resourceErrors:", rerr[:120])
    print("h1:", h1[:60], "| horizontalOverflow:", hov[:120])
    print("stdout tail:", out[-260:].replace("\n", " ")[:260])
    print()
