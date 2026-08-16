#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, brier_score_loss, log_loss
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler


def build_features(df: pd.DataFrame):
    df = df.sort_values("timestamp").reset_index(drop=True)

    opens = df["open"].astype(float).to_numpy()
    highs = df["high"].astype(float).to_numpy()
    lows = df["low"].astype(float).to_numpy()
    closes = df["close"].astype(float).to_numpy()

    rows = []
    labels = []

    for i in range(15, len(df) - 1):
        r = np.maximum(1e-9, highs[i] - lows[i])
        body = closes[i] - opens[i]
        upper = max(0.0, highs[i] - max(opens[i], closes[i]))
        lower = max(0.0, min(opens[i], closes[i]) - lows[i])

        mean_range = np.mean(
            np.maximum(
                1e-9,
                highs[i - 14:i + 1] - lows[i - 14:i + 1]
            )
        )

        x = np.zeros(8, dtype=float)
        x[0] = body / mean_range
        x[1] = upper / mean_range
        x[2] = lower / mean_range
        x[3] = (closes[i] - closes[i - 3]) / mean_range
        x[4] = (closes[i] - closes[i - 7]) / mean_range
        x[5] = np.mean(closes[i - 4:i + 1]) - np.mean(closes[i - 9:i + 1])
        x[5] /= mean_range
        x[6] = (
            np.polyfit(
                np.arange(10),
                closes[i - 9:i + 1],
                1
            )[0] / mean_range
        )
        x[7] = r / mean_range - 1.0

        rows.append(x)
        labels.append(1 if closes[i + 1] > opens[i + 1] else 0)

    return np.asarray(rows), np.asarray(labels)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("csv")
    parser.add_argument("--out", default="models")
    args = parser.parse_args()

    df = pd.read_csv(args.csv)

    required = {"timestamp", "open", "high", "low", "close"}
    missing = required - set(df.columns)

    if missing:
        raise SystemExit(
            f"Missing required columns: {sorted(missing)}"
        )

    X, y = build_features(df)

    cut = int(len(X) * 0.8)

    X_train = X[:cut]
    y_train = y[:cut]
    X_test = X[cut:]
    y_test = y[cut:]

    model = Pipeline([
        ("scale", StandardScaler()),
        (
            "classifier",
            LogisticRegression(
                max_iter=2000,
                class_weight="balanced"
            )
        )
    ])

    model.fit(X_train, y_train)

    probabilities = model.predict_proba(X_test)[:, 1]
    predictions = probabilities >= 0.5

    report = {
        "train_rows": int(len(X_train)),
        "test_rows": int(len(X_test)),
        "accuracy": float(
            accuracy_score(y_test, predictions)
        ),
        "log_loss": float(
            log_loss(y_test, probabilities)
        ),
        "brier": float(
            brier_score_loss(y_test, probabilities)
        ),
        "approved_for_live": False
    }

    output = Path(args.out)
    output.mkdir(parents=True, exist_ok=True)

    (output / "walk_forward_report.json").write_text(
        json.dumps(report, indent=2),
        encoding="utf-8"
    )

    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
