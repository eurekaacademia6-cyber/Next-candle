# Historical training

Required CSV columns:
timestamp,open,high,low,close

Optional:
volume,spread,symbol,timeframe

Run:
python research/train_next_candle.py data/EURUSD_M5.csv --out models

Walk-forward:
python research/walk_forward.py data/EURUSD_M5.csv --folds 5

Do not approve a model for live use merely because a single backtest looks good.
The correct sequence is chronological train -> validation -> untouched test -> paper trading -> live review.
