# Architecture

Camera
-> live frame
-> chart ROI
-> candle reconstruction
-> 10-15 candle rolling sequence
-> feature extraction

Feature layers:
- candle geometry
- trajectory
- trend
- momentum
- volatility
- price location
- structure proxies
- liquidity sweep proxies
- regime

Decision layers:
- rule ensemble
- adaptive learner
- confidence
- agreement gate
- UP / DOWN / NO EDGE

The camera-only application cannot directly observe hidden order-book liquidity,
true spread, tick-by-tick order flow, or macro news. Those require external data.
