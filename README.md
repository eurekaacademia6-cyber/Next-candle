# Next Candle AI Ultimate 3.0 - Clean Rebuild

Standalone Android camera application for live next-candle analysis.

The app:
- watches a visible candlestick chart with the phone camera
- attempts to reconstruct a rolling set of up to 15 candles
- analyzes candle geometry
- analyzes short/long trajectory
- detects price-structure proxies
- detects visible price-based liquidity sweep proxies
- analyzes momentum and volatility
- classifies a simple market regime
- combines independent components
- outputs UP, DOWN, or NO EDGE

Important:
This is a research/analysis application, not a guaranteed predictor. A camera-only system cannot observe hidden order-book liquidity, true spread, tick flow, or news that is not visible in the chart.

Build:
- JDK 17
- Gradle 8.13
- Android SDK 36
