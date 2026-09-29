#!/usr/bin/env python3
"""Sign a declarative protocol package with ECDSA P-256/SHA-256."""
from __future__ import annotations
import argparse, base64, hashlib, json
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec


def canonical(manifest: dict, payload: bytes) -> bytes:
    h = hashlib.sha256(payload).hexdigest()
    values = [
        manifest['id'], manifest['manufacturer'], manifest['platform'],
        str(manifest['version']), str(manifest['minAppVersion']),
        ','.join(sorted(manifest.get('modelPatterns', []))), manifest.get('trust',''), manifest.get('source',''), h
    ]
    return ('\n'.join(values)).encode()


def load_key(path: Path):
    if not path.exists():
        raise SystemExit(f"Signing key does not exist: {path}")
    return serialization.load_pem_private_key(path.read_bytes(), password=None)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('input', type=Path)
    ap.add_argument('output', type=Path)
    ap.add_argument('--key', type=Path, required=True)
    ap.add_argument('--public-key-output', type=Path)
    args = ap.parse_args()

    root = json.loads(args.input.read_text())
    payload = base64.b64decode(root['payloadBase64'])
    root['payloadSha256'] = hashlib.sha256(payload).hexdigest()
    root['trust'] = 'SIGNED'
    root['source'] = 'REMOTE'
    key = load_key(args.key)
    sig = key.sign(canonical(root, payload), ec.ECDSA(hashes.SHA256()))
    root['signature'] = base64.b64encode(sig).decode()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(root, separators=(',', ':'), ensure_ascii=False))

    if args.public_key_output:
        pub = key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        args.public_key_output.parent.mkdir(parents=True, exist_ok=True)
        args.public_key_output.write_text(base64.b64encode(pub).decode() + '\n')

if __name__ == '__main__':
    main()
