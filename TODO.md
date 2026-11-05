# Lyrelo TODO

## Branding
- [ ] Replace the 23 alternate app icons in the icon picker (glass, neon, monochrome). They still show BitChord art.
- [ ] Replace the in-app logo (`ic_logo`) on Home, onboarding and Replay.
- [ ] Replace the TV banner (`tv_banner`).

## Turned off until Lyrelo has its own
- [ ] **In-app updater:** off via `UPDATES_ENABLED` in `AppUpdateChecker.kt`. Point `LATEST_RELEASE_URL` and the `UPDATE_APK_PREFIX` build field at Lyrelo releases, then turn it back on.
- [ ] **Listen Together server:** no default is set, so the screen shows "not available yet". Deploy `backend/`, then set `LISTEN_TOGETHER_SERVER` in `local.properties` or the build environment.
- [ ] **Listen Together invite links:** these still use `bitchord.kushagrasingh.in` (`JamInviteLink.kt`, `JamInvite.kt`, the manifest deep link). Move them to a Lyrelo domain along with the server.
- [ ] **Presence (live "apps open" count):** off via `ENABLED` in `AndroidPresence.kt`. Maybe replace with our own endpoint later; it must never point at BitChord's server.

## Still points at BitChord
- [ ] Discord: "Visit BitChord" button and BitChord's Discord application ID (`DiscordRPC.kt`, `DiscordApplication.kt`).
- [ ] README badges and banner (release, downloads and "Listening now" are BitChord's).

## Language learning
- [ ] Translate the new language picker strings. They're English-only for now; other languages fall back to English.
- [ ] Use the learning language. It's saved and editable, but nothing reads it yet: translations depend only on the native language.
