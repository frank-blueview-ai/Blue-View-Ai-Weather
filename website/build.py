#!/usr/bin/env python3
"""
Renders website/privacy.html from PRIVACY.md.

The policy is generated, never hand-edited: Play compares the published policy
against the Data safety declaration, so the live page and the repo copy must not
be able to drift apart. Edit PRIVACY.md and re-run this.
"""
import html
import os
import re

import markdown

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "PRIVACY.md")
OUT = os.path.join(ROOT, "website", "privacy.html")

body = markdown.markdown(
    open(SRC, encoding="utf-8").read(),
    extensions=["tables", "toc", "sane_lists", "attr_list"],
)

# The .md leads with an H1; the page supplies its own chrome around it.
title = "Privacy Policy — Blue View Weather"
m = re.search(r"<h1[^>]*>(.*?)</h1>", body, re.S)
if m:
    title = html.unescape(re.sub(r"<[^>]+>", "", m.group(1))).strip()

page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="How the Blue View Weather Android app handles location and other data.">
<link rel="icon" href="/assets/icon.png">
<link rel="stylesheet" href="/assets/site.css">
<meta name="theme-color" content="#0B0E1C">
<meta name="robots" content="index,follow">
</head>
<body>

<header class="top">
  <div class="wrap">
    <a class="brand" href="/"><img src="/assets/icon.png" alt=""> Blue View Weather</a>
    <nav><a href="/">Home</a><a href="/privacy">Privacy</a></nav>
  </div>
</header>

<main class="prose">
{body}
</main>

<footer>
  <div class="wrap">
    <div class="links">
      <a href="/">Home</a>
      <a href="https://github.com/frank-blueview-ai/Blue-View-Ai-Weather">Source</a>
      <a href="mailto:frank@blueview.ai">frank@blueview.ai</a>
    </div>
    <div>&copy; 2026 BlueView / Frank Perez.</div>
  </div>
</footer>

</body>
</html>
"""
open(OUT, "w", encoding="utf-8").write(page)
print(f"wrote {OUT} ({len(page)} bytes) from {os.path.basename(SRC)}")
