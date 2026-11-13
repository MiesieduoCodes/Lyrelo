# Lyrelo UI audit — defects

Audit date: 2026-11-14. Device: Redmi Note 14 Pro+ (HyperOS, 1220×2712, 433 dpi, font scale 0.81).
App: dev build of commit `d2177f9c` (installed 2026-11-06).
Method: design critique (hierarchy, spacing, alignment, consistency) and WCAG 2.1 AA review
(contrast ≥ 4.5:1, targets ≥ 48dp, labels, large font) of the screenshots in `before/`, plus the Compose code.

Severity: **high** = broken, misleading, legal, or blocks a core task. **med** = clearly wrong, hurts
readability or accessibility. **low** = polish.

Paths are relative to `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/` (written `sharedUi:`)
or `app/src/main/java/com/music/bitchord/` (written `app:`).

## Defects

| # | Screen | Problem | Sev | Fix at |
|---|--------|---------|-----|--------|
| 1 | Player, lyrics | DJ gets stuck. The badge showed "Lyrelo DJ is picking your mix…" indefinitely while an unrelated song played. The session's host lives in the activity, there's no timeout, and resuming the same song never sends a song-start event, so it never ends. (`player-current-dark`, `player-playing-dark`) | high | `app:playback/DjSession.kt:68-86`, `app:MainActivity.kt:1100` |
| 2 | Lyrics | DJ badge and caption sit on top of the song title and artist header. (`lyrics-dark`) | high | `sharedUi:ui/player/NowPlayingScreen.kt:1948`, `sharedUi:ui/player/DjBadge.kt:57` |
| 3 | Player | DJ badge overlaps the "Playing from…" header text and the drag handle. Both are visible at once. (`player-current-dark`) | high | `sharedUi:ui/player/NowPlayingScreen.kt:1948` |
| 4 | Whole app | Bundles Apple's SF Pro Display (`res/font/sf_pro_display_*.otf`). Its license forbids redistribution, so it can't ship in a GPL app. | high | `app:ui/theme/Theme.kt:67`, `app/src/main/res/font/` |
| 5 | DJ | Voice plays on `USAGE_ASSISTANT`, which HyperOS sets far lower than music, so the DJ is too quiet. | high | `app:playback/LyreloDj.kt:239-242` |
| 6 | Lyrics | Mixed alignment. Single-line lyrics are left-aligned while wrapped lines are centered, and the translation under a wrapped line stays left. (`lyrics-translated-dark`, `lyrics-controls-dark`) | high | `sharedUi:ui/player/PlayerLyrics.kt:205` (`voiceStyle` centered), `:2257`, `:2265` |
| 7 | Settings | The footer links go to BitChord's GitHub, developer page, Discord and website, plus "~YouTube Music & Listen Together Backend". The name is lowercase "lyrelo". | high | `app:ui/screens/SettingsSheet.kt:1744-1769` |
| 8 | Lyrics | The Translate and Learn buttons (Lyrelo's core features) are hidden until you tap an empty spot. Tapping a line seeks instead, so they're hard to find. | med | `sharedUi:ui/player/NowPlayingScreen.kt:3852` |
| 9 | Player | DJ X is 20dp, "Change vibe" about 26dp tall. Both are under the 48dp minimum. | med | `sharedUi:ui/player/DjBadge.kt:75-95` |
| 10 | Learn | Close button is 24dp, chips about 30dp, Learn toggle 34dp, indicator X 15dp. All under 48dp. | med | `sharedUi:ui/player/LearnControls.kt:154`, `:207`, `:324-328`, `:365` |
| 11 | Home | Tile subtitles are white at 80% on Practice/Moods/Continue (3.8–4.0:1, below 4.5:1). | med | `sharedUi:ui/screens/HomeScreen.kt:1285` |
| 12 | Home | Tiles have a fixed 92dp height with one-line title and subtitle. At large font sizes the text clips. | med | `sharedUi:ui/screens/HomeScreen.kt:1262` |
| 13 | Home | Tile colors, streak orange and DJ colors are hardcoded hex values, not theme tokens, so they ignore light mode and accent settings. | med | `sharedUi:ui/screens/HomeScreen.kt:1178-1243`, `sharedUi:ui/player/DjBadge.kt:48-50` |
| 14 | DJ | "Lyrelo DJ", "Change vibe", "End DJ", every caption and every spoken line are hardcoded English. | med | `sharedUi:ui/player/DjBadge.kt:69,76,89`, `app:playback/DjSession.kt:79-177`, `app:playback/LyreloDj.kt:91-112` |
| 15 | Settings | Native and learning language can't be reached from Settings → Language or Settings → Learning, only from the lyrics translate dialog. | med | `app:ui/screens/SettingsSheet.kt` (Language and Learning sections) |
| 16 | Explore | Mood cards crop the playlist artwork mid-word ("pump-up pop", "Feel-Good Hip Hop and R&B"). | med | `sharedUi:ui/screens/` mood grid card (Explore) |
| 17 | Player | The Automix and "stats for nerds" debug text overflows the artwork and is cut off ("can't ta…"). | med | `sharedUi:ui/player/NowPlayingScreen.kt` (stats overlay) |
| 18 | Lyrics | Inactive lyric lines are about 2.0:1 on bright artwork, and the "This phone" label 2.5:1. | med | `sharedUi:ui/player/PlayerLyrics.kt` (inactive alpha), `NowPlayingScreen.kt` (output label) |
| 19 | Pickers | Player style previews barely differ: Vinyl shows no record, and Immersive and Classic look the same. The preview lags a tap behind the selected chip. (`picker-player-*`) | med | `app:ui/screens/PlayerStyleScreen.kt` |
| 20 | Lyrics | The previous line is cut off under the header. Its top half shows above the active line. (`lyrics-translated-dark`) | low | `sharedUi:ui/player/NowPlayingScreen.kt` (lyrics top fade) |
| 21 | Home | The avatar floats above the header row instead of lining up with the logo and streak. (`home-dark`) | low | `sharedUi:ui/screens/HomeScreen.kt:499-531` |
| 22 | Home | The mini player covers the next section's title ("New…"). | low | `sharedUi:ui/screens/HomeScreen.kt` (bottom content padding) |
| 23 | DJ | The caption uses a fixed `14.sp`, the orb animates forever and ignores "Reduce animation", and the caption has no live-region announcement for screen readers. | low | `sharedUi:ui/player/DjBadge.kt:105`, `:122-135` |
| 24 | Learn | Fixed `13.sp` labels and hardcoded white/black colors (no tokens). | low | `sharedUi:ui/player/LearnControls.kt:316,356` |
| 25 | Settings | When scrolled, the floating back button sits over the list text with no scrim. (`settings-dark-2`) | low | `app:ui/screens/SettingsSheet.kt` (header) |
| 26 | Home | The Top Picks second card sometimes shows no artwork (empty dark card). (`home-dark`) | low | `sharedUi:ui/screens/HomeScreen.kt:797` (`HeroCard`) |
| 27 | Lyrics | Tapping a lyric line seeks to it with no hint, so a tap meant to show the controls jumps the song instead. | low | `sharedUi:ui/player/PlayerLyrics.kt` |

## Contrast checks

| Element | FG | BG | Ratio | Need | Pass |
|---|---|---|---|---|---|
| Tile title, white / Continue `#0E7490` | `#FFFFFF` | `#0E7490` | 5.36 | 4.5 | ✅ |
| Tile subtitle, white 80% / Continue | `#CFE3E9` | `#0E7490` | 4.04 | 4.5 | ❌ |
| Tile subtitle, white 80% / Practice `#B45309` | `#F0DDCE` | `#B45309` | 3.81 | 4.5 | ❌ |
| Tile subtitle, white 80% / Moods `#15803D` | `#D0E6D8` | `#15803D` | 3.82 | 4.5 | ❌ |
| DJ caption, white / black 55% on bright art | `#FFFFFF` | `#5A561D` | 7.54 | 4.5 | ✅ |
| Learn hint, white 72% / panel on bright art | `#C7C7BD` | `#383612` | 7.24 | 4.5 | ✅ |
| "This phone", white ~50% / olive player | `#BDB890` | `#7A7020` | 2.50 | 4.5 | ❌ |
| Translation teal / olive player | `#5FD3B0` | `#6B6A1E` | 3.08 | 4.5 | ❌ |
| Inactive lyric line / olive | `#9F9E6D` | `#6B6A1E` | 2.05 | 3.0 (large) | ❌ |
| Dark secondary text `#8E8E93` / `#0D0D0F` | | | 5.95 | 4.5 | ✅ |
| Light secondary text `#6E6E73` / `#F7F7F9` | | | 4.74 | 4.5 | ✅ |

## Not covered yet

- Light mode and largest-font "before" passes. The phone was in use during the audit. These get
  captured after the Phase 1 install and are compared against the code instead.
- Lyric style picker, Learn panel open, language picker dialog, first launch, Practice list, DJ
  with captions while speaking. Phone time ran out. The code review covers these components.
