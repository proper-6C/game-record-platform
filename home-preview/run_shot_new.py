# -*- coding: utf-8 -*-
"""批量自检 3 个新预览页（desktop only）"""
import os, subprocess

skill = r"C:\Users\27754\AppData\Local\Doubao\User Data\Default\.doubao\agent_mode\workspace\.skills\html\scripts\shot.py"
html_dir = r"D:\soft\code\Save_code\First-db\home-preview"
py = r"C:\Users\27754\AppData\Local\Doubao\User Data\sandbox_runtime\bases\c98c5042338ed152c6f10ecd8591889f\python\python.exe"

targets = ["荣耀榜效果预览.html", "对局数据可视化效果预览.html", "收藏夹效果预览.html"]
for t in targets:
    p = os.path.join(html_dir, t)
    print("=" * 60)
    print("TARGET:", t)
    r = subprocess.run([py, skill, "--only", "desktop", p], capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = r.stdout or ""
    # 提取关键 lint 字段
    import re
    for key in ["consoleErrors", "responsiveness", "brokenLinks", "clickable", "mainImage", "clippedText", "lintIssues", "shots", "exit"]:
        m = re.search(r'"' + key + r'":\s*(\[[^\]]*\]|\{[^}]*\}|true|false|null|"[^"]*")', out)
        if m:
            print(f"  {key}: {m.group(1)[:300]}")
    print("  exit:", r.returncode)
