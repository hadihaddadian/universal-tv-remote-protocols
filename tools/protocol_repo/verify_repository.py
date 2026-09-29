#!/usr/bin/env python3
from __future__ import annotations
import argparse, base64, hashlib, json
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

def canonical(m,payload):
    return ('\n'.join([m['id'],m['manufacturer'],m['platform'],str(m['version']),str(m['minAppVersion']),','.join(sorted(m.get('modelPatterns',[]))),m.get('trust',''),m.get('source',''),hashlib.sha256(payload).hexdigest()])).encode()

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('root',type=Path); ap.add_argument('--public-key',type=Path,required=True); a=ap.parse_args()
    pub=serialization.load_der_public_key(base64.b64decode(a.public_key.read_text().strip()))
    manifest=json.loads((a.root/'manifest.json').read_text())
    assert manifest['schemaVersion']==1
    for r in manifest['protocols']:
        p=a.root/r['path']; o=json.loads(p.read_text()); payload=base64.b64decode(o['payloadBase64'])
        assert o['id']==r['id'] and o['version']==r['version']
        assert o['payloadSha256'].lower()==r['payloadSha256'].lower()==hashlib.sha256(payload).hexdigest()
        pub.verify(base64.b64decode(o['signature']), canonical(o,payload), ec.ECDSA(hashes.SHA256()))
    print(f'Protocol repository verification: OK ({len(manifest["protocols"])} releases)')
if __name__=='__main__': main()
