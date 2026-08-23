#!/usr/bin/env bash
# Publishes website/ to weather.blueview.ai.
#
#   ./deploy.sh user@host
#
# Idempotent: rsync only ships what changed. It never touches nginx config —
# installing the vhost and issuing the certificate is a one-time manual step,
# documented in nginx-weather.blueview.ai.conf.
set -euo pipefail

TARGET=${1:?usage: ./deploy.sh user@host}
DOCROOT=/var/www/weather.blueview.ai
DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# Regenerate the policy so a stale privacy.html can never be published.
python3 "$DIR/build.py"

echo "==> deploying to $TARGET:$DOCROOT"
ssh "$TARGET" "sudo mkdir -p $DOCROOT && sudo chown -R \$USER:\$USER $DOCROOT"
rsync -az --delete \
  --exclude 'deploy.sh' --exclude 'build.py' --exclude 'nginx-*.conf' --exclude 'README.md' \
  "$DIR"/ "$TARGET:$DOCROOT/"
ssh "$TARGET" "sudo chown -R www-data:www-data $DOCROOT"

echo "==> verifying"
for path in / /privacy; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "https://weather.blueview.ai$path" || echo 000)
  echo "    https://weather.blueview.ai$path -> $code"
done
