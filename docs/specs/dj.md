# Lyrelo DJ — spec

Status: Phase 2 of the 2026-11-14 session. Owner: Lyrelo. Covers `LyreloDj` (the voice) and
`DjSession` (the DJ mix).

## Problem

The DJ is Lyrelo's most personal feature: a radio host that plays your music, talks between
songs and teaches a phrase from what you just heard. Today it lets people down in ways that make
it feel broken rather than charming:

- **It gets stuck.** Starting a vibe can leave the badge on "Lyrelo DJ is picking your mix…"
  indefinitely while unrelated music plays (seen in the audit, `docs/audit/ui-defects.md` #1).
- **You can't hear it.** The voice plays on the Assistant audio channel. HyperOS sets that far
  below music, so at normal volume the DJ is buried.
- **It sounds like a robot.** Phone text-to-speech, even the best installed voice, reads like a
  screen reader, not a host.
- **It repeats itself and talks too much.** The same lines come back every time, and it can talk
  after every song.
- **It dies with the screen.** The session's "host" lives in the activity. Close or recreate the
  activity and the DJ has no player, but still shows as active.

## How it works today

**`LyreloDj`** (object, main thread)
- Created in `BitChordApplication`. `ListeningRecorder` calls `onPlaying(song)` on each playback
  sample. When the song changes, and the DJ is on in Settings, no DJ mix is running and Learn is
  not active, it talks every 3rd song.
- `announce()` builds lines: streak (once a day), "Here's a phrase from that song", the original
  phrase, "It means", the translation, then the announcement. Every English line is
  machine-translated at runtime into the learning or native language.
- It speaks with `android.speech.tts.TextToSpeech`, picks the "best male voice" by name hints,
  pitch 0.92 and rate 0.98, with 350 ms pauses. Audio attributes are `USAGE_ASSISTANT` +
  `CONTENT_TYPE_SPEECH`, and it ducks music with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
- The TTS engine is created once and never released.

**`DjSession`** (object, main thread)
- `start(host, name)` from Home or Library. `Host` is an anonymous object in `MainActivity`
  (`play`, `queueNext`, `playNow`, `setVolume`).
- The opening mix is a YouTube Music radio seeded from one of your 5 most-played songs, or Quick
  Picks as a fallback.
- Every 5–10 songs it fetches a mood playlist (chill, romance, feel good, energy, party, focus),
  queues 8 songs, and talks when the first of them starts. "Change vibe" plays a new mood now.
- Any song outside the session's known set ends the session.
- When a named song starts, it is faded from 0 to 30% under the voice, then up to 100% when the
  voice ends.

## Goals

1. **Reliable:** picking a vibe starts music within 5 s on a normal connection, and the sheet
   closes. The badge never shows a state that isn't true for more than 15 s.
2. **Audible:** at the phone's normal music volume, the voice is clearly louder than the music
   under it (music at 10–15%, voice at full media volume × the user's DJ voice volume).
3. **Natural:** with the natural voice downloaded, English DJ lines use Kokoro `af_heart` by
   default, with no gap between the end of a song and the voice.
4. **Pleasant:** the DJ talks every 2–4 songs (Normal), never says the same line twice in a row,
   and doesn't repeat a line within a session while unused variants remain.
5. **Survives the activity:** the DJ keeps going with the app in the background or the activity
   recreated, and stops cleanly when playback stops.

## Non-goals

- **A cloud voice or an account.** Everything runs on the phone. Network use is limited to the
  one-time voice download and YouTube Music.
- **Natural voices in every language now.** sherpa-onnx's Kokoro supports English (and Chinese)
  only. Spanish, French, Italian, Portuguese, Hindi and Japanese use the best installed Android
  voice. Russian and Kazakh get Piper voices.
- **Supertonic 3.** A later candidate (MIT code, OpenRAIL-M weights), noted in TODO.md.
- **Human-written translations of the DJ lines.** This phase moves them into `strings.xml` so
  translators can work on them later. Runtime translation remains the fallback.
- **Talking during Learn.** A loop or slowed song means someone is studying. The DJ stays quiet,
  as it does today.

## User stories

- As a learner, I want to tap a vibe and hear music right away so that the DJ feels instant.
- As a listener on a noisy bus, I want the DJ's voice to be clearly louder than the music so that I
  understand the phrase it teaches.
- As a learner of Spanish, I want the phrase said in Spanish and then explained in my language so
  that I learn it in context.
- As someone who lets the DJ run for an hour, I want it to vary what it says and not talk after
  every song so that it doesn't get annoying.
- As a user, I want "Change vibe" and "End DJ mix" to work from the notification and after I
  switch apps so that I don't have to reopen the player.
- As a user with no internet, I want a clear message instead of a stuck badge.
- As a user who edits the queue, I want the DJ to step aside politely, not fight me.

## Requirements

### P0 — must have

**A. Basics and edge cases**
- [ ] Picking a vibe starts playback immediately (the first playable song of the mix plays as
      soon as it is fetched) and the vibe sheet closes. This holds for every vibe and the Learn
      vibe.
- [ ] "Change vibe" and "End DJ mix" work from the player badge **and** as media-notification
      custom actions, including after the app has been backgrounded.
- [ ] No internet: the badge says "Can't reach YouTube Music. Check your connection." and the
      session ends after 15 s. It is never stuck on "picking your mix…".
- [ ] A song with no lyrics: the DJ skips the phrase lesson and still announces.
- [ ] TTS or voice failure: music keeps playing at full volume, and the voice is skipped
      silently (logged).
- [ ] The user edits the queue (adds, removes or plays something else): the DJ says nothing more
      and the session ends with a short caption ("DJ mix ended. Your queue is up next.").
- [ ] Phone call or audio-focus loss: speech stops, and music behaves as it does without the DJ.
      Headphones unplugged: playback pauses as usual, and the DJ does not talk into the speaker.

**B. Loudness**
- [ ] The voice plays on `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`, at full stream volume × the DJ
      voice volume.
- [ ] Music ducks to 12% (range 10–15%) with a 300 ms fade down and comes back up over 600 ms
      after the voice. A named new song fades in under the voice at that low level.
- [ ] Settings → Learning → DJ voice volume: 50–150% (default 100%). Above 100% is applied as
      gain on the synthesized PCM, clamped to avoid clipping.

**C. Natural voice**
- [ ] sherpa-onnx (Apache 2.0) offline TTS via its Android AAR.
- [ ] Voices:
  - Kokoro multi-lang (Apache 2.0): `af_heart` (default), `af_bella`, `am_michael`,
    `am_fenrir`, for English.
  - Piper `kk_KZ-issai-high` (dataset CC-BY-4.0) for Kazakh.
  - Piper `ru_RU-dmitri-medium` (dataset CC0) for Russian.
  - **Not shipped:** `ru_RU-irina-medium` (dataset licence "Unknown") and
    `ru_RU-ruslan-medium` (CC BY-NC-SA, non-commercial). `ru_RU-denis` is non-commercial.
- [ ] Models are not in the APK. On first DJ use: "Get the natural DJ voice (~140 MB)" with
      progress, cancel, and Delete in Settings. Downloaded from sherpa-onnx's official GitHub
      releases and checked against a pinned size and SHA-256 before it is used.
- [ ] Fallback when no model is downloaded: Android TTS with the highest-quality installed voice
      (prefers `QUALITY_VERY_HIGH`, then network voices) at rate 0.97.
- [ ] The next line's audio is synthesized while the current song plays, so the voice starts
      without a gap.
- [ ] Voice picker with a Preview button in DJ settings.

**D. Lines**
- [ ] Short, casual, radio-host lines: at most 2 short sentences per moment, spoken in chunks
      with ~250 ms gaps.
- [ ] Several variants each for intro, phrase lesson, streak, vibe change and outro. Never the
      same line twice in a row, and no repeat within a session until all variants have been
      used.
- [ ] Talk frequency: Chatty (every 1–2 songs), Normal (every 2–4, default), Quiet (every 4–6).
- [ ] Phrase lesson: the phrase in the song's language (in that language's voice when one is
      downloaded), then its meaning in the native language.
- [ ] Line templates live in `strings.xml` (English base). Runtime translation only for
      languages without a translation.

**E. Lifecycle**
- [ ] DJ state moves into a controller owned by `PlaybackService` and exposed as a `StateFlow`.
      The activity only observes and sends commands.
- [ ] The session survives activity destruction and stops when playback stops or the service
      ends.
- [ ] The TTS engine (Android or sherpa-onnx) is released when the DJ stops.
- [ ] Unit tests: the line picker (no repeats), talk frequency and vibe switching.

### P1 — nice to have
- Ducking that follows the voice's own loudness envelope instead of a fixed level.
- A second English voice per gender for variety within one session.

### P2 — later
- Supertonic 3 voices (more languages, small model).
- Human-written DJ line translations.
- A "learning room" DJ shared across Listen Together.

## Success metrics (on-device testing; no analytics in Lyrelo)

- Vibe → first audio: < 5 s on Wi-Fi in 10/10 tries.
- Stuck badge: 0 occurrences in a 1-hour session with app switching and screen off.
- Loudness: on the Redmi Note 14 Pro+ at normal music volume, the voice is clearly louder than the
  music in 5/5 announcements (subjective check, plus the duck level confirmed in logs).
- Repetition: across 30 announcements in one session, no line repeats while unused variants
  remain (unit-tested).

## Open questions

- **Engineering, non-blocking:** Kokoro multi-lang v1.1 renumbers speakers. Speaker IDs are
  read from the model's voice list at load time, not hardcoded.
- **Engineering, non-blocking:** the int8 model (~140 MB download) is used if its quality is
  acceptable. Otherwise fp32 (~330 MB). Decided by listening to the samples in
  `docs/voice-samples/`.
- **Product, non-blocking:** should the DJ's Chatty setting also teach a phrase every time, or
  only when the song had lyrics with a usable line?

## Phasing

1. A, B and E: reliability, loudness and lifecycle. Shippable on Android TTS alone.
2. D: lines and frequency.
3. C: natural voice download, picker and samples.
