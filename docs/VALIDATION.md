# Validation

Never claim a win rate from in-sample results.

Use chronological train/test splits and walk-forward evaluation.

Measure:
- accuracy
- precision by direction
- Brier score
- log loss
- calibration
- coverage at confidence thresholds
- expectancy under a clearly defined outcome rule

A model must remain disabled for live use until its out-of-sample metrics have been reviewed.
