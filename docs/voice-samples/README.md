# DJ voice samples

Made with the exact models and sherpa-onnx version (1.13.8) the app downloads and runs,
so what you hear here is what the DJ sounds like. Speed 0.97.

| File | Voice | Language | Licence |
|---|---|---|---|
| `en-kokoro-af_heart.wav` | Kokoro af_heart (default) | English | Apache 2.0 |
| `en-kokoro-af_bella.wav` | Kokoro af_bella | English | Apache 2.0 |
| `en-kokoro-am_michael.wav` | Kokoro am_michael | English | Apache 2.0 |
| `en-kokoro-am_fenrir.wav` | Kokoro am_fenrir | English | Apache 2.0 |
| `ru-piper-dmitri.wav` | Piper ru_RU-dmitri-medium | Russian | MIT model, CC0 data |
| `kk-piper-issai.wav` | Piper kk_KZ-issai-high | Kazakh | MIT model, CC-BY-4.0 data (ISSAI) |

English line: "Hey, Lyrelo DJ here. Starting with Despacito by Luis Fonsi. Quick phrase from
that song: pasito a pasito. That means step by step. You're on a five-day streak. Nice."

Not included, and not shipped: ru_RU-irina (dataset licence unknown), ru_RU-ruslan and
ru_RU-denis (non-commercial).

Why 1.13.8 and not 2.x: sherpa-onnx 2.0 dropped its built-in espeak-ng, so Kokoro silently
skips words that aren't in its dictionary (song titles, artist names) and Piper voices can't
speak at all without an external phonemizer.
