#!/usr/bin/env python3
"""Build manifest.json for the static protocol CDN from signed package JSON files."""
from __future__ import annotations
import argparse, base64, hashlib, json, os
from datetime import datetime, timezone
from pathlib import Path


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('root', type=Path); args=ap.parse_args()
    root=args.root
    releases=[]
    for path in sorted(root.glob('protocols/*/*.json')):
        if path.name == 'latest.json':
            continue
        obj=json.loads(path.read_text())
        payload=base64.b64decode(obj['payloadBase64'])
        if hashlib.sha256(payload).hexdigest().lower()!=obj['payloadSha256'].lower():
            raise SystemExit(f'hash mismatch: {path}')
        payload_obj=json.loads(payload.decode())
        rel={
          'id':obj['id'],'version':obj['version'],'status':obj.get('status','STABLE'),
          'minAppVersion':obj['minAppVersion'],'manufacturer':obj['manufacturer'],
          'platform':obj['platform'],'modelPatterns':obj.get('modelPatterns',[]),
          'supportedManufacturers':payload_obj.get('supportedManufacturers',[]),
          'discoveryHints':payload_obj.get('discoveryHints',[]),
          'transports':payload_obj.get('transports',[]),'authMode':payload_obj.get('authMode','none'),
          'capabilities':payload_obj.get('capabilities',[]),'payloadSha256':obj['payloadSha256'],
          'sizeBytes':path.stat().st_size,'path':str(path.relative_to(root)).replace('\\','/')
        }
        releases.append(rel)
    releases.sort(key=lambda x:(x['id'],x['version']))
    epoch = os.environ.get('SOURCE_DATE_EPOCH')
    if epoch and epoch.isdigit():
        generated_at = datetime.fromtimestamp(int(epoch), timezone.utc).replace(microsecond=0).isoformat().replace('+00:00', 'Z')
    else:
        generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace('+00:00', 'Z')
    out={'schemaVersion':1,'repository':'universal-tv-remote-protocols','generatedAt':generated_at,'protocols':releases}
    (root/'manifest.json').write_text(json.dumps(out,indent=2,ensure_ascii=False)+'\n')
    print(f'Wrote {root/"manifest.json"} with {len(releases)} releases')

if __name__=='__main__': main()
