import argparse,json,pandas as pd,numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score,log_loss,brier_score_loss
from train_next_candle import make_features
p=argparse.ArgumentParser();p.add_argument('csv');p.add_argument('--folds',type=int,default=5);a=p.parse_args();df=pd.read_csv(a.csv);X,y=make_features(df);n=len(X);fs=n//(a.folds+1);rows=[]
for k in range(1,a.folds+1):
 te=fs*k;end=min(fs*(k+1),n)
 if te<200 or end<=te:continue
 m=Pipeline([('scale',StandardScaler()),('model',LogisticRegression(max_iter=2000,class_weight='balanced'))]);m.fit(X[:te],y[:te]);p=m.predict_proba(X[te:end])[:,1];yy=y[te:end];rows.append({'fold':k,'train_rows':te,'test_rows':len(yy),'accuracy':float(accuracy_score(yy,p>=.5)),'log_loss':float(log_loss(yy,p)),'brier':float(brier_score_loss(yy,p))})
print(json.dumps(rows,indent=2))
