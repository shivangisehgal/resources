# References

## Transcript

The study notes were created from the uploaded transcript:

`tactiq.io free youtube transcript` for the Logger LLD video.

## Code

The code is based on the Logger implementation at:

https://github.com/shubhkpatel/Low-Level-Design/tree/main/src/main/java/org/nailyourinterview/lld/logger

The source was checked against the repository structure and individual Java files.

## Important implementation caveats

1. `LogLevel` contains `TRACE`, but there is no `TraceHandler`.
2. `LogHandlerConfiguration.addAppenderForLevel()` uses a `switch`, so adding a new level requires updating this configuration method.
3. The handler-to-appender relationship is implemented as an observer-style subscription rather than explicit `Subject`/`Observer` interfaces.
4. `FileAppender.append()` is synchronized to serialize file writes.
5. `CopyOnWriteArrayList` is used for the appender collection.
