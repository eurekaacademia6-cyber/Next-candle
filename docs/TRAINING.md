# Empirical training

CSV required columns: timestamp, open, high, low, close. Optional: volume, spread, symbol, timeframe.

Train:
python research/train_next_candle.py data/EURUSD_M5.csv --out models

Walk-forward:
python research/walk_forward.py data/EURUSD_M5.csv --folds 5

The sample target is next-candle direction. For production research, prefer barrier labels such as +k ATR before -k ATR, with a separate NO EDGE class. Never randomly shuffle time series. Keep a final untouched holdout period.

A model is never auto-approved for live use. Review log loss, Brier score, calibration, precision at high-confidence thresholds, coverage, and stability by month/regime/session.
