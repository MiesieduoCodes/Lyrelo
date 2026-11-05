# Lyrelo TODO

## Turned off until Lyrelo has its own
- [ ] **In-app updater:** off via `UPDATES_ENABLED` in `AppUpdateChecker.kt`. Point `LATEST_RELEASE_URL` and the `UPDATE_APK_PREFIX` build field at Lyrelo releases, then turn it back on.
- [ ] **Listen Together server:** no default is set, so the screen shows "not available yet". Deploy `backend/`, then set `LISTEN_TOGETHER_SERVER` in `local.properties` or the build environment.
- [ ] **Listen Together invite links:** these still use `bitchord.kushagrasingh.in` (`JamInviteLink.kt`, `JamInvite.kt`, the manifest deep link). Move them to a Lyrelo domain along with the server.
- [ ] **Presence (live "apps open" count):** off via `ENABLED` in `AndroidPresence.kt`. Maybe replace with our own endpoint later; it must never point at BitChord's server.

## Still says BitChord
- [ ] Discord's status card shows BitChord's app name, because it uses BitChord's Discord application ID (`DiscordApplication.kt`). Needs our own Discord application.
- [ ] README body, badges, `Logo.png` and `Banner.png`.
- [ ] Website, backend and native code (`web/`, `backend/`, `native/`). Not part of the phone app.

## Language learning
- [ ] Translate the language picker, Learn panel and Light icon strings. They're English-only for now; other languages fall back to English.
- [ ] Use the learning language. It's saved and editable, but nothing reads it yet: translations depend only on the native language.
