# Next Candle AI Ultimate 2.0

This is a standalone Android camera application plus an offline research/training pipeline.

## Live camera engine
The app continuously analyzes frames and estimates a rolling candle sequence. It combines:
- candle body and wick geometry
- momentum
- trajectory slope and acceleration
- volatility expansion/compression
- swing/high-low location proxies
- liquidity sweep proxies
- engulfing/rejection geometry
- trend/regime classification
- rule ensemble
- adaptive local learner

The output is UP, DOWN, or NO EDGE.

## Important limitation
A camera-only app cannot directly see hidden order-book liquidity, spread, tick flow, or macro news. The liquidity features are price-action proxies inferred from the visible chart.

## Model training
Use real broker-specific OHLC data. Do not shuffle time series. Keep a final untouched out-of-sample period. Measure calibration, log loss, Brier score, precision at high confidence, coverage, and expectancy rather than raw accuracy alone.
