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
- [ ] Gradle property names such as `bitchord.bundleWear` (kept on purpose for now).

## Licensing
- [ ] **TV and Wear OS still bundle Apple's SF Pro** (`tv/.../TvTheme.kt`, `wearos/.../Theme.kt`, their `res/font/`). It can't ship in a GPL app. The phone app now uses Geologica + Onest (OFL); do the same there.

## Language learning
- [ ] **Translations:** Lyrelo's own strings (Learn, DJ, learning stats, theme settings) are English-only. They're listed in `app/src/test/resources/untranslated-keys.txt`; remove each key from there as it gets translated.
- [ ] **DJ voices in more languages:** natural voices exist for English (Kokoro), Russian and Kazakh (Piper). Spanish, French, Italian, Portuguese, Hindi and Japanese use the phone's voice. sherpa-onnx only supports Kokoro for English/Chinese.
- [ ] **Later candidate, not now:** Supertonic 3 voices (MIT code, OpenRAIL-M weights). Check the OpenRAIL-M use restrictions before shipping.
- [ ] **sherpa-onnx is pinned to 1.13.8.** 2.x dropped espeak-ng: Kokoro would skip song titles and names, and Piper can't speak. Revisit if 2.x gains a phonemizer we can bundle.

## Done (kept for context)
- [x] The learning language is read everywhere it matters: translation, learning stats, the DJ, Practice and the Learn vibe.
