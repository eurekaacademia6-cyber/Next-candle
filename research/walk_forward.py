import argparse
import json
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, log_loss, brier_score_loss
from train_next_candle import features

p=argparse.ArgumentParser(); p.add_argument("csv"); p.add_argument("--folds",type=int,default=5); a=p.parse_args()
df=pd.read_csv(a.csv); X,y=features(df); n=len(X); fold=max(1,n//(a.folds+1)); out=[]
for k in range(1,a.folds+1):
    train_end=fold*k; test_end=min(fold*(k+1),n)
    if train_end<200 or test_end<=train_end: continue
    model=Pipeline([("scale",StandardScaler()),("model",LogisticRegression(max_iter=2000,class_weight="balanced"))])
    model.fit(X[:train_end],y[:train_end]); p=model.predict_proba(X[train_end:test_end])[:,1]; yy=y[train_end:test_end]
    out.append({"fold":k,"train":train_end,"test":len(yy),"accuracy":float(accuracy_score(yy,p>=.5)),"log_loss":float(log_loss(yy,p)),"brier":float(brier_score_loss(yy,p))})
print(json.dumps(out,indent=2))
