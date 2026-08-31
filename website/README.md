# weather.blueview.ai

Static marketing site for the Blue View Weather Android app, plus the privacy
policy that Google Play requires at a public URL.

```
index.html          landing page
privacy/index.html  GENERATED — do not edit; run build.py
build.py            renders the privacy page from ../PRIVACY.md
assets/         css, icon, screenshots
deploy.sh       rsync to the server
nginx-weather.blueview.ai.conf   vhost, with setup steps in its header
```

## Why privacy.html is generated

Play compares the published policy against the Data safety declaration. If the
live page and `PRIVACY.md` drift apart, that is a misdeclaration. Editing
`PRIVACY.md` and re-running `build.py` makes drift impossible.

## Deploy

The site is hosted on **GitHub Pages**, deployed by
`.github/workflows/website.yml` on any push to `main` that touches `website/`
or `PRIVACY.md`. There is no server to log into and nothing shared with
production infrastructure.

The workflow refuses to publish if `privacy/index.html` has drifted from
`PRIVACY.md`, or if any absolute local path would break under a subpath.

### Custom domain

Live at **https://weather.blueview.ai** — DNS is a CNAME to
`frank-blueview-ai.github.io`, and GitHub issues the TLS certificate.

`website/CNAME` is part of the deployed artifact on purpose: with
Actions-based publishing the custom domain set in repo settings can be dropped
by a later deploy, and that file re-asserts it every time. Do not delete it.

### Self-hosting instead

`deploy.sh` and `nginx-weather.blueview.ai.conf` remain for anyone who would
rather serve it from their own box. They are not used by the live site.

## The /privacy URL is load-bearing

`https://weather.blueview.ai/privacy` is printed inside the app (About screen)
and submitted to the Play Console. It must keep working. The nginx config maps
the extensionless path explicitly rather than relying on a redirect.
