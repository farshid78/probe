# Android core compile smoke test

Run:

```bash
./tools/android-core-smoke-tests/run.sh
```

This compiles the real parser, `ScanService`, and `Store` Java classes against minimal Android/JSON test doubles. It is a syntax and type smoke test only; it does not replace Android Gradle Plugin compilation, lint, instrumentation tests, emulator execution, or device testing.
