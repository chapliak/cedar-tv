#!/usr/bin/env python3
"""Lists the languages whose "Latest videos" category is empty (-> NO_VIDEO in Api.kt).
Run: python3 tools/probe_video_languages.py   (needs internet, ~1 min)"""
import json, urllib.request
from concurrent.futures import ThreadPoolExecutor
B = "https://b.jw-cdn.org/apis/mediator/v1/"
get = lambda u: json.load(urllib.request.urlopen(u, timeout=25))
codes = [l["code"] for l in get(B + "languages/E/web")["languages"]]
def n(c):
    try: return c, len(get(f"{B}categories/{c}/LatestVideos?detailed=1")["category"].get("media", []))
    except Exception: return c, 0
with ThreadPoolExecutor(12) as ex: res = list(ex.map(n, codes))
print(sorted(c for c, k in res if k == 0))
