@echo off
echo {\n  \"question\": \"SpringBoot 如何开启 SSE？\",\n  \"conversationId\": \"test-001\"\n} > request.json
curl -X POST "http://localhost:9999/api/chat/completions" -H "Content-Type: application/json" -d @request.json
del request.json