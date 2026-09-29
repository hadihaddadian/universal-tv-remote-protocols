> Historical document. It is retained for traceability and must not override README.md, ARCHITECTURE.md, IMPLEMENTATION_STATUS.md, CURRENT_STATUS.md, or SECURITY.md.

# V28 — Built-in Protocol Catalog

The APK now contains a structured built-in catalog. Resolution order remains:

1. Compatible installed protocol
2. Built-in protocol/profile
3. Verified remote protocol package

Generic fallback profiles are deliberately low priority and are not claims of
undocumented vendor-specific protocols. They provide safe candidates such as
Android/Google TV Remote v2, IR, mDNS/UPnP/DLNA-style discovery and generic
transport fallbacks.

Included built-in families:
- Android TV Remote v2 shared profile
- Google TV generic profile
- Samsung WebSocket family placeholder/profile boundary
- LG webOS SSAP profile boundary
- Sony IRCC profile boundary
- Philips JointSpace profile boundary
- Roku ECP profile boundary
- DLNA/UPnP generic control
- Generic IR TV
- Generic TV fallback

Protocol storage is keyed by ProtocolId. Multiple brands may reference one
shared protocol package; changing brands does not cause duplicate downloads.
Remote packages remain declarative and are validated before installation.
