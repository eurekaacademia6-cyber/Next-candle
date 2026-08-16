# Next Candle AI Ultimate

Continuously analyzes a live chart through the phone camera. The pipeline is camera -> chart/candle reconstruction -> feature engine -> trajectory -> structure/liquidity proxies -> momentum/volatility/regime -> rule ensemble -> adaptive ML -> confidence gate -> UP/DOWN/NO EDGE.

## Live contract
- Maintain a rolling window of up to 15 visible candles.
- The current candle may be used only as evolving state.
- The target is the next candle after the current candle.
- At candle rollover, the window advances without using future data.

## Independent analytical layers
- candle geometry
- sequence momentum
- price trajectory and slope
- local extrema and range location
- liquidity sweep proxies
- engulfing/rejection geometry
- volatility/compression/expansion
- trend/regime
- ensemble agreement
- adaptive probability model

## Reality constraint
A camera-only app cannot observe hidden DOM/order-book liquidity, true spread, tick flow or macro news. Those require external data and are intentionally excluded from this standalone build.
