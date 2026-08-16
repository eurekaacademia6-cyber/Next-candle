from __future__ import annotations
import argparse,json
from pathlib import Path
import numpy as np,pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.metrics import accuracy_score,log_loss,brier_score_loss
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

def make_features(df):
 df=df.sort_values('timestamp').copy();o,h,l,c=[df[x].astype(float).to_numpy() for x in ['open','high','low','close']];X=[];y=[]
 for i in range(15,len(df)-1):
  cl=c[:i+1];rg=np.maximum(1e-9,h[:i+1]-l[:i+1]);bd=c[:i+1]-o[:i+1];mr=max(1e-9,np.mean(rg[-15:]));mb=max(1e-9,np.mean(np.abs(bd[-15:])));x=np.zeros(28);rng=max(1e-9,h[i]-l[i]);up=max(0,h[i]-max(o[i],c[i]));lo=max(0,min(o[i],c[i])-l[i]);x[0]=bd[i]/mr;x[1]=up/mr;x[2]=lo/mr;x[3]=(c[i]-l[i])/rng*2-1;x[4]=rng/mr-1;x[5]=abs(bd[i])/mb-1;x[6]=(c[i]-c[i-3])/mr;x[7]=(c[i]-c[i-6])/mr;m5=np.mean(c[i-4:i+1]);m10=np.mean(c[i-9:i+1]);x[8]=(c[i]-m5)/mr;x[9]=(m5-m10)/mr;x[10]=np.polyfit(np.arange(5),c[i-4:i+1],1)[0]/mr;x[11]=np.polyfit(np.arange(10),c[i-9:i+1],1)[0]/mr;last5=rg[i-4:i+1];prev5=rg[i-9:i-4];x[12]=np.mean(last5)/max(1e-9,np.mean(prev5))-1;hh=np.max(h[:i]);ll=np.min(l[:i]);x[13]=(c[i]-hh)/mr;x[14]=(c[i]-ll)/mr;x[15]=float(l[i]<ll and c[i]>ll);x[16]=float(h[i]>hh and c[i]<hh)
  if i>=1:x[17]=1 if c[i]>o[i] and c[i-1]<o[i-1] and o[i]<=c[i-1] and c[i]>=o[i-1] else (-1 if c[i]<o[i] and c[i-1]>o[i-1] and o[i]>=c[i-1] and c[i]<=o[i-1] else 0)
  x[18]=(lo-up)/mr;x[20]=(rng-np.mean(last5))/mr;g=[];loss=[]
  for j in range(i-9,i+1):d=c[j]-c[j-1];g.append(max(0,d));loss.append(max(0,-d))
  avgl=max(1e-9,np.mean(loss));x[21]=(100-100/(1+np.mean(g)/avgl))/50-1;x[22]=np.clip(x[10]*3,-1,1);x[23]=np.clip(x[11]*3,-1,1);x[24]=(c[i]-m10)/mr;x[25]=np.std(last5)/mr;x[26]=((c[i]-c[i-1])-(c[i-1]-c[i-2]))/mr;x[27]=1;X.append(x);y.append(int(c[i+1]>o[i+1]))
 return np.asarray(X),np.asarray(y)

def main():
 ap=argparse.ArgumentParser();ap.add_argument('csv');ap.add_argument('--out',default='models');ap.add_argument('--holdout',type=float,default=.2);args=ap.parse_args();df=pd.read_csv(args.csv);need={'timestamp','open','high','low','close'};miss=need-set(df.columns);assert not miss,f'Missing columns: {sorted(miss)}';X,y=make_features(df);cut=int(len(X)*(1-args.holdout));Xtr,Xte,ytr,yte=X[:cut],X[cut:],y[:cut],y[cut:];models=[('logistic',Pipeline([('scale',StandardScaler()),('model',LogisticRegression(max_iter=2000,class_weight='balanced'))])),('hist_gb',HistGradientBoostingClassifier(max_depth=4,learning_rate=.04,max_iter=250,l2_regularization=.2))];report={'rows':len(X),'train':len(Xtr),'test':len(Xte),'models':{}};best=None;bestloss=1e9
 for name,m in models:
  m.fit(Xtr,ytr);p=m.predict_proba(Xte)[:,1];loss=log_loss(yte,p);report['models'][name]={'accuracy':float(accuracy_score(yte,p>=.5)),'log_loss':float(loss),'brier':float(brier_score_loss(yte,p))};
  if loss<bestloss:best=(name,m);bestloss=loss
 out=Path(args.out);out.mkdir(parents=True,exist_ok=True);(out/'walk_forward_report.json').write_text(json.dumps(report,indent=2))
 if best[0]=='logistic':
  sc=best[1].named_steps['scale'];lr=best[1].named_steps['model'];payload={'version':1,'feature_count':28,'mean':sc.mean_.tolist(),'scale':sc.scale_.tolist(),'weights':lr.coef_[0].tolist(),'bias':float(lr.intercept_[0]),'metrics':report['models'][best[0]],'approved_for_live':False};(out/'model.json').write_text(json.dumps(payload,indent=2))
 print(json.dumps({'best_model':best[0],'report':report},indent=2))
if __name__=='__main__':main()
