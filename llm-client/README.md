# LLM Chat Client (Java 8 Swing)

A self-contained, dependency-free Java 8 Swing desktop client for a generic LLM
HTTP API. It sends prompts, keeps conversation history, and displays a chat-style
transcript. Network calls run off the Event Dispatch Thread so the UI never
freezes.

## Architecture

Strictly layered, no circular dependencies:

```
client.domain   models + contracts   (no UI, no networking)
client.engine   LLM engine + logic   (depends on domain + Java SE only)
client.ui       Swing presentation   (depends on domain + engine)
client.app      Main entry point     (wires everything together)
```

### Project tree

```
src/
  client/domain/
    ChatMessage.java          Immutable message (role, content, timestamp)
    Conversation.java         Ordered list of messages
    LLMRequest.java           Prompt + optional conversation context
    LLMResponse.java          Reply or failure (success / errorMessage)
    LLMClient.java            Contract: LLMResponse sendRequest(LLMRequest)
    ConversationModel.java    Contract: tracks history, builds requests
  client/engine/
    Json.java                 Minimal hand-written JSON escape/extract helper
    HttpLLMClient.java        Real client over java.net.HttpURLConnection
    MockLLMClient.java        Offline canned/echo client for testing the UI
    DefaultConversationModel.java   Default ConversationModel implementation
  client/ui/
    ChatClientFrame.java      Main window (transcript, input, Send, status)
  client/app/
    Main.java                 Entry point; chooses real vs mock client
```

## How to run

Requires a Java 8 (or later) JDK. No build tool needed.

```sh
# Compile
javac -d out $(find src -name "*.java")

# Run in mock mode (no network, default) — works fully offline
java -cp out client.app.Main

# Run against a real endpoint
java -cp out client.app.Main https://your-endpoint/v1/chat YOUR_API_KEY
```

With no arguments the app starts with `MockLLMClient`, which echoes your prompt
back so you can exercise the UI without a server.

### Real endpoint contract

`HttpLLMClient` POSTs JSON of the form:

```json
{ "prompt": "...", "messages": [ {"role":"user","content":"..."} ] }
```

with `Authorization: Bearer <apiKey>` when a key is supplied. It reads the reply
from the first string field named `content`, `response`, `text`, `message` or
`completion`; if none is present it falls back to the raw response body. Network
errors, non-2xx status, and empty bodies are surfaced in the status bar instead
of crashing.

## Constraints

- Java 8 only — no `var`, text blocks, modules, or post-8 APIs.
- Standard Java SE only — no Maven/Gradle, no third-party JSON library.
- Verified: compiles cleanly with `javac 1.8.0` (`-Xlint:all`, no warnings).
```
