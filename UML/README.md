# UML Diagrams

This folder contains two PlantUML diagrams:

- `LoggerDesign.puml` — class diagram showing the main classes, interfaces, relationships and patterns.
- `LoggerFlow.puml` — sequence diagram showing the flow of `logger.error(...)`.

## Render

Open either `.puml` file in any PlantUML-compatible editor/plugin.

### Class diagram

The key relationships are:

```text
Logger
  |
  v
LogHandler chain
  |
  v
Handler
  |
  +--> LogAppender
  |       |
  |       v
  |   LogFormatter
  |
  +--> next Handler
```

### Sequence

```text
Client
  -> Logger
  -> Handler Chain
  -> ErrorHandler
  -> Console/File Appenders
  -> Formatter
  -> Output
```
