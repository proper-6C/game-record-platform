# -*- coding: utf-8 -*-
"""游戏对局记录平台 - 接口全流程验证脚本"""
import base64
import json
import urllib.request
import uuid

BASE = "http://localhost:8080"

# 1x1 红色 PNG（测试上传用）
PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)


def req(method, path, body=None, token=None):
    url = BASE + path
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))


def upload_record(token, game_name="王者荣耀"):
    """构造 multipart 上传对局"""
    boundary = uuid.uuid4().hex
    fields = [
        ("gameName", game_name),
        ("gameMode", "排位"),
        ("matchDate", "2026-10-04"),
        ("result", "win"),
        ("rank", "王者"),
        ("description", "自动化测试上传的对局"),
    ]
    buf = b""
    for k, v in fields:
        buf += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n"
                % (boundary, k, v)).encode("utf-8")
    buf += ("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\n"
            "Content-Type: image/png\r\n\r\n" % boundary).encode("utf-8")
    buf += PNG + b"\r\n"
    buf += ("--%s--\r\n" % boundary).encode("utf-8")
    r = urllib.request.Request(BASE + "/api/record/upload", data=buf, method="POST",
                               headers={
                                   "Content-Type": "multipart/form-data; boundary=" + boundary,
                                   "Authorization": "Bearer " + token,
                               })
    with urllib.request.urlopen(r, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


ok = True
passed = 0
total = 0


def check(name, cond, detail=""):
    global ok, passed, total
    total += 1
    if cond:
        passed += 1
    else:
        ok = False
    print(("[PASS] " if cond else "[FAIL] ") + name + (" | " + detail if detail else ""))
    return cond


ok = True

# 1. 注册（已存在则登录）
s, r = req("POST", "/api/user/register", {"username": "player1", "password": "123456", "nickname": "玩家一"})
ok &= check("注册接口", r.get("code") in (200, 400), json.dumps(r, ensure_ascii=False))

# 2. 登录
s, r = req("POST", "/api/user/login", {"username": "player1", "password": "123456"})
token = r.get("data", {}).get("token", "")
ok &= check("登录接口", r.get("code") == 200 and bool(token), "token=" + (token[:8] + "..." if token else "无"))

# 3. 错误密码登录应失败
s, r = req("POST", "/api/user/login", {"username": "player1", "password": "wrong"})
ok &= check("错误密码被拒绝", r.get("code") == 400, json.dumps(r, ensure_ascii=False))

# 4. 匿名列表（应为空或包含数据）
s, r = req("GET", "/api/record/list?page=1&size=10")
ok &= check("匿名列表", r.get("code") == 200, json.dumps(r, ensure_ascii=False))

# 5. 未登录写操作应被拦截
s, r = req("POST", "/api/comment/add", {"recordId": 1, "content": "x"})
ok &= check("未登录评论被拦截", r.get("code") == 401, json.dumps(r, ensure_ascii=False))

# 6. 上传对局
r = upload_record(token)
rec = r.get("data") or {}
ok &= check("上传对局", r.get("code") == 200 and rec.get("id"), json.dumps(r, ensure_ascii=False))
record_id = rec.get("id")

# 7. 详情（匿名可看）
s, r = req("GET", "/api/record/%s" % record_id)
d = r.get("data") or {}
ok &= check("详情接口", r.get("code") == 200 and d.get("gameName") == "王者荣耀",
            "uploaderName=" + str(d.get("uploaderName")) + ", likeCount=" + str(d.get("likeCount")))

# 8. 搜索列表
s, r = req("GET", "/api/record/list?page=1&size=10&gameName=" + urllib.parse.quote("王者"))
ok &= check("按游戏名搜索", r.get("code") == 200 and len(r.get("data", {}).get("list", [])) >= 1)

# 9. 评论
s, r = req("POST", "/api/comment/add", {"recordId": record_id, "content": "这波操作很秀！"}, token=token)
c = r.get("data") or {}
ok &= check("发表评论", r.get("code") == 200 and c.get("id"), json.dumps(r, ensure_ascii=False))
comment_id = c.get("id")

# 10. 回复楼中楼
s, r = req("POST", "/api/comment/add", {"recordId": record_id, "parentId": comment_id, "content": "回复：确实"}, token=token)
ok &= check("楼中楼回复", r.get("code") == 200)

# 11. 敏感词过滤
s, r = req("POST", "/api/comment/add", {"recordId": record_id, "content": "加微信 代练 了解一下"}, token=token)
filtered = (r.get("data") or {}).get("content", "")
ok &= check("敏感词过滤", r.get("code") == 200 and "加微信" not in filtered and "代练" not in filtered and "***" in filtered,
            json.dumps(r, ensure_ascii=False))

# 12. 评论列表（含回复）
s, r = req("GET", "/api/comment/list?recordId=%s&page=1&size=10" % record_id)
cl = r.get("data", {}).get("list", [])
ok &= check("评论分页列表", r.get("code") == 200 and len(cl) >= 1 and any(len(x.get("replyList", [])) > 0 for x in cl),
            "一级评论数=" + str(len(cl)))

# 13. 点赞对局
s, r = req("POST", "/api/like/toggle", {"targetId": record_id, "targetType": "record"}, token=token)
ok &= check("点赞对局", r.get("data", {}).get("liked") is True and r.get("data", {}).get("likeCount") == 1,
            json.dumps(r, ensure_ascii=False))

# 14. 重复点赞防刷（toggle 第二次应取消）
s, r = req("POST", "/api/like/toggle", {"targetId": record_id, "targetType": "record"}, token=token)
ok &= check("重复点赞=取消", r.get("data", {}).get("liked") is False and r.get("data", {}).get("likeCount") == 0,
            json.dumps(r, ensure_ascii=False))

# 15. 再点赞，验证详情 liked 回显
s, r = req("POST", "/api/like/toggle", {"targetId": record_id, "targetType": "record"}, token=token)
s, r = req("GET", "/api/record/%s" % record_id, token=token)
ok &= check("详情回显已点赞", r.get("data", {}).get("liked") is True and r.get("data", {}).get("likeCount") == 1)

# 16. 点赞评论
s, r = req("POST", "/api/like/toggle", {"targetId": comment_id, "targetType": "comment"}, token=token)
ok &= check("点赞评论", r.get("data", {}).get("liked") is True, json.dumps(r, ensure_ascii=False))

# 17. 点赞状态接口
s, r = req("GET", "/api/like/status?targetId=%s&targetType=record" % record_id, token=token)
ok &= check("点赞状态查询", r.get("data", {}).get("liked") is True)

# 18. 删除对局（级联清理）
s, r = req("DELETE", "/api/record/%s" % record_id, token=token)
ok &= check("删除对局", r.get("code") == 200)
s, r = req("GET", "/api/record/%s" % record_id)
ok &= check("删除后详情404", r.get("code") == 404)

print()
print("=== 结果: %s (通过 %d/%d) ===" % ("全部通过" if ok else "存在失败", passed, total))
