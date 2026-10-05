import urllib.request
import os

urls = {
    "coverA.jpg": "https://aka.doubaocdn.com/s/rrb1267ubA",
    "coverC.jpg": "https://aka.doubaocdn.com/s/MuUwoGemSU",
    "coverD.jpg": "https://aka.doubaocdn.com/s/UhZJba0sQ0",
}
dst = r"D:\soft\code\Save_code\First-db\home-preview\assets"
for name, u in urls.items():
    path = os.path.join(dst, name)
    try:
        req = urllib.request.Request(u, headers={"User-Agent": "Mozilla/5.0"})
        data = urllib.request.urlopen(req, timeout=30).read()
        with open(path, "wb") as f:
            f.write(data)
        print(name, len(data), "bytes")
    except Exception as e:
        print(name, "FAIL", repr(e))
