from __future__ import annotations
import argparse
import json
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, log_loss, brier_score_loss


def features(df: pd.DataFrame):
    df = df.sort_values("timestamp").reset_index(drop=True)
    o = df["open"].to_numpy(float); h = df["high"].to_numpy(float)
    l = df["low"].to_numpy(float); c = df["close"].to_numpy(float)
    X, y = [], []
    for i in range(15, len(df)-1):
        rng = np.maximum(1e-9, h[:i+1]-l[:i+1])
        body = c[:i+1]-o[:i+1]
        avg_rng = max(1e-9, rng[-15:].mean())
        avg_body = max(1e-9, np.abs(body[-15:]).mean())
        rr = max(1e-9, h[i]-l[i])
        upper = max(0.0, h[i]-max(o[i],c[i]))
        lower = max(0.0, min(o[i],c[i])-l[i])
        hh = h[:i].max(); ll = l[:i].min()
        x = np.zeros(32, float)
        x[0] = body[i]/avg_rng; x[1] = upper/avg_rng; x[2] = lower/avg_rng
        x[3] = 2*(c[i]-l[i])/rr-1; x[4] = rr/avg_rng-1; x[5] = abs(body[i])/avg_body-1
        x[6] = (c[i]-c[i-3])/avg_rng; x[7] = (c[i]-c[i-6])/avg_rng
        x[8] = (c[i]-c[i-4:i+1].mean())/avg_rng; x[9] = (c[i-4:i+1].mean()-c[i-9:i+1].mean())/avg_rng
        x[10] = np.polyfit(np.arange(5), c[i-4:i+1], 1)[0]/avg_rng
        x[11] = np.polyfit(np.arange(10), c[i-9:i+1], 1)[0]/avg_rng
        x[12] = rng[-5:].mean()/max(1e-9, rng[-10:-5].mean())-1
        x[13] = (c[i]-hh)/avg_rng; x[14] = (c[i]-ll)/avg_rng
        x[15] = 1.0 if l[i] < ll and c[i] > ll else 0.0
        x[16] = 1.0 if h[i] > hh and c[i] < hh else 0.0
        if i >= 1:
            bull = c[i] > o[i] and c[i-1] < o[i-1] and o[i] <= c[i-1] and c[i] >= o[i-1]
            bear = c[i] < o[i] and c[i-1] > o[i-1] and o[i] >= c[i-1] and c[i] <= o[i-1]
            x[17] = 1 if bull else (-1 if bear else 0)
        x[18] = (lower-upper)/avg_rng
        direction = c[i] > o[i]; streak = 0
        for j in range(i, max(-1, i-6), -1):
            if (c[j] > o[j]) == direction and c[j] != o[j]: streak += 1
            else: break
        x[19] = streak/5 if direction else -streak/5
        x[20] = (rr-rng[-5:].mean())/avg_rng
        gains=[]; losses=[]
        for j in range(i-9,i+1):
            d=c[j]-c[j-1]; gains.append(max(0,d)); losses.append(max(0,-d))
        al=max(1e-9,np.mean(losses)); rsi=100-100/(1+np.mean(gains)/al); x[21]=rsi/50-1
        x[22]=np.sign(x[10])*min(1,abs(x[10])); x[23]=np.clip(x[11],-1,1)
        x[24]=(c[i]-c[i-9:i+1].mean())/avg_rng; x[25]=rng[-5:].std()/avg_rng
        x[26]=((c[i]-c[i-1])-(c[i-1]-c[i-2]))/avg_rng; x[27]=1.0
        x[28]=abs(x[10]); x[29]=(c[i]-hh)/max(1e-9,hh-ll); x[30]=(c[i]-ll)/max(1e-9,hh-ll); x[31]=1.0
        X.append(x); y.append(1 if c[i+1] > o[i+1] else 0)
    return np.asarray(X), np.asarray(y)


def main():
    ap=argparse.ArgumentParser(); ap.add_argument("csv"); ap.add_argument("--out",default="models"); ap.add_argument("--holdout",type=float,default=0.2)
    a=ap.parse_args(); df=pd.read_csv(a.csv)
    req={"timestamp","open","high","low","close"}
    miss=req-set(df.columns)
    if miss: raise SystemExit(f"Missing columns: {sorted(miss)}")
    X,y=features(df); cut=int(len(X)*(1-a.holdout)); Xtr,Xte=X[:cut],X[cut:]; ytr,yte=y[:cut],y[cut:]
    models={
      "logistic":Pipeline([("scale",StandardScaler()),("model",LogisticRegression(max_iter=2000,class_weight="balanced"))]),
      "hist_gb":HistGradientBoostingClassifier(max_depth=4, learning_rate=0.04, max_iter=200, l2_regularization=0.2)
    }
    report={"rows":len(X),"train":len(Xtr),"test":len(Xte),"models":{}}
    best=None
    for name,m in models.items():
        m.fit(Xtr,ytr); p=m.predict_proba(Xte)[:,1]
        report["models"][name]={"accuracy":float(accuracy_score(yte,p>=0.5)),"log_loss":float(log_loss(yte,p)),"brier":float(brier_score_loss(yte,p))}
        if best is None or report["models"][name]["log_loss"] < report["models"][best]["log_loss"]: best=name
    out=Path(a.out); out.mkdir(parents=True,exist_ok=True); (out/"walk_forward_report.json").write_text(json.dumps(report,indent=2))
    (out/"LIVE_APPROVAL_REQUIRED.txt").write_text("Do not mark a model approved_for_live until chronological walk-forward and calibration review is complete.\n")
    print(json.dumps({"best_model":best,**report},indent=2))

if __name__ == "__main__": main()
