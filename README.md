# Next Candle AI Ultimate 1.0

Standalone Android camera application for continuous next-candle prediction. No MT5, broker, bridge or internet is required.

The app attempts to reconstruct 10-15 visible candles and combines candle geometry, trajectory, structure proxies, liquidity sweep proxies, momentum, volatility and regime analysis with an adaptive local model.

Outputs: UP / DOWN / NO EDGE.

NO EDGE is intentionally a first-class output. The app does not guarantee accuracy or profitability.

## Build
GitHub Actions -> Build -> download `NextCandleAI-debug-apk` artifact.

## Research
See `research/` and `docs/` for the historical training and walk-forward validation pipeline.
