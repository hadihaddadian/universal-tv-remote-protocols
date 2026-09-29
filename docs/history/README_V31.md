> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# UniversalTVRemote V31

V31 consolidates the 15 architecture improvements into the existing V30 structure.
The authoritative path is Discovery -> Fingerprint -> ProtocolManager -> BindingRegistry -> Strategy -> Transport.
AI/voice commands terminate in a canonical command model and policy boundary.
Built-in protocols are preferred over cache and remote updates. Remote protocol packages remain declarative and signed.
