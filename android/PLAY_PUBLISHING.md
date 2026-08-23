# Automated Play uploads

Once configured, pushing an `android-v*` tag builds the AAB and uploads it to the
Play **internal** track as a **draft**. Nothing goes live without you.

## What is already wired

- `.github/workflows/android-build.yml` has a `publish-play` job.
- It is **inert until you add the `PLAY_SERVICE_ACCOUNT_JSON` secret** — the job
  logs a notice and skips, so CI stays green in the meantime.
- Tag pushes upload to `internal`. Never to production: promotion stays a
  deliberate act in the Play Console, or a manual run of this workflow with
  `play_track=production`.
- Uploads are created as `draft`, which is reviewable and abortable.
- The key is written to `/tmp`, validated as JSON, and deleted in an `always()`
  step so it does not survive the runner even if the upload fails.

## What you have to do once

These steps cannot be automated — they create the credential that grants upload
rights to your developer account.

### 1. First upload must be manual

Google will not accept an API upload for an app that has never had a build
uploaded through the Console. Upload `app-play-release.aab` by hand once
(Play Console → Testing → Internal testing → Create new release). After that,
every subsequent release can go through this pipeline.

### 2. Create the service account

1. Play Console → **Setup → API access**.
2. Follow the link to the linked Google Cloud project (create one if prompted).
3. In Google Cloud → **IAM & Admin → Service Accounts → Create service account**.
   Name it something like `play-ci-uploader`. No Google Cloud roles are needed —
   the permission that matters is granted in the Play Console, not in Cloud IAM.
4. On the new account → **Keys → Add key → Create new key → JSON**. A `.json`
   file downloads. This is a credential: treat it like a password.

### 3. Grant it upload rights in the Play Console

Play Console → **Users and permissions → Invite new user** → paste the service
account's email → grant, for this app only:

- **Release → Release to testing tracks** (needed)
- **Release → Release to production** only if you want the pipeline to be able to
  push production. Leaving it off is the safer default and still allows the
  internal-track automation.

Permissions can take a few minutes to propagate.

### 4. Add the secret

GitHub → repo → **Settings → Secrets and variables → Actions → New repository
secret**:

- Name: `PLAY_SERVICE_ACCOUNT_JSON`
- Value: the **entire contents** of the downloaded JSON file

Then delete the downloaded file from your machine.

### 5. Verify without releasing anything

Actions → *Build Android APK & AAB* → **Run workflow** → set `play_track` to
`internal`. It builds and uploads a draft. If the credential is wrong you get a
clear API error and nothing is published.

## Notes

- `versionCode` is derived from `versionName` (1.2.1 → 10201). Play permanently
  rejects a reused code, so every upload needs a version bump — including one
  replacing a rejected build.
- The AAB is deliberately **not** attached to the public GitHub release. Only the
  GitHub-flavour APK is public; the AAB is a private CI artifact.
- If you ever rotate the service-account key, update the secret. If the key
  leaks, revoke it in Google Cloud immediately — it can publish to your account.
