# Redirect URL performance test

This folder uses k6 to check the redirect endpoint latency target:

```text
95% of redirect requests should complete under 200ms.
```

## Run

1. Start PostgreSQL and the application.

2. Create one short URL and copy the returned `shortCode`.

```bash
curl -X POST http://localhost:10689/urls \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"https://www.youtube.com/watch?v=spring\"}"
```

3. Run the k6 test with that short code.

```bash
k6 run -e BASE_URL=http://localhost:10689 -e SHORT_CODE=<shortCode> performance/redirect-url.k6.js
```

The test disables redirect following, so it measures this API's `302 Found` response instead of measuring the target website.
