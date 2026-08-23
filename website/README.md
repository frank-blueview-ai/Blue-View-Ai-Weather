# weather.blueview.ai

Static marketing site for the Blue View Weather Android app, plus the privacy
policy that Google Play requires at a public URL.

```
index.html      landing page
privacy.html    GENERATED — do not edit; run build.py
build.py        renders privacy.html from ../PRIVACY.md
assets/         css, icon, screenshots
deploy.sh       rsync to the server
nginx-weather.blueview.ai.conf   vhost, with setup steps in its header
```

## Why privacy.html is generated

Play compares the published policy against the Data safety declaration. If the
live page and `PRIVACY.md` drift apart, that is a misdeclaration. Editing
`PRIVACY.md` and re-running `build.py` makes drift impossible.

## Deploy

```bash
./deploy.sh user@bvos-host
```

Before the first deploy:

1. **DNS** — an A record for `weather.blueview.ai` pointing at the server
   (`bvos.blueview.ai` currently resolves to `34.56.90.115`). Nothing else works
   until this propagates.
2. **vhost** — install `nginx-weather.blueview.ai.conf`; steps are in its header.
3. **TLS** — `sudo certbot --nginx -d weather.blueview.ai`. Requires DNS first.

## The /privacy URL is load-bearing

`https://weather.blueview.ai/privacy` is printed inside the app (About screen)
and submitted to the Play Console. It must keep working. The nginx config maps
the extensionless path explicitly rather than relying on a redirect.
