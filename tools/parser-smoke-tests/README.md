# Parser smoke tests

Run with a JDK (no Android SDK or Gradle required):

```bash
./tools/parser-smoke-tests/run.sh
```

The harness compiles the real `Fmt`, `Pkg`, `Op`, and `Extractor` source files against minimal test doubles for Android `Rect`, `AccessibilityNodeInfo`, and `org.json`. It exercises normalization, currency handling, extraction truncation, duplicate suppression, and the privacy-safe diagnostic summary.

**Scope limitation:** these tests do not emulate the Android Accessibility framework, real operator apps, multi-window behavior, device lifecycle, UI, Gradle packaging, APK signing, or Google Play Protect. A passing smoke test is not a real-device integration test.
