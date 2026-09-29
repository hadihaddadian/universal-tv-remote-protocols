#!/usr/bin/env python3
"""Fast repository invariants; Gradle remains the authoritative compiler/test runner."""
from pathlib import Path
import json, re, sys, os
from datetime import datetime
root=Path(__file__).resolve().parents[1]; errors=[]
build=(root/'app/build.gradle.kts').read_text(encoding='utf-8')
code=re.search(r'versionCode\s*=\s*(\d+)',build); name=re.search(r'versionName\s*=\s*"([^"]+)"',build)
if not code or not name:
    errors.append('versionCode/versionName metadata is missing')
if name and not name.group(1).isdigit():
    errors.append('versionName must be an integer with no decimal component')
if code and name and int(code.group(1)) != int(name.group(1)):
    errors.append(f'Android versionCode and versionName must be identical, found {code.group(1)} / {name.group(1)}')
if code and int(code.group(1)) != 63:
    errors.append(f'expected V63 Android versionCode 63, found {code.group(1)}')
if name and name.group(1)!='63': errors.append(f'expected V62 release version 62, found {name.group(1)}')
readme=(root/'README.md').read_text(encoding='utf-8')
if not readme.startswith('# Universal TV Remote V63'): errors.append('README.md is not V63')
if name:
    release_match=re.match(r'^# Universal TV Remote V(\d+)\s*$', readme.splitlines()[0].strip())
    if not release_match or release_match.group(1) != name.group(1):
        errors.append('human-facing README release version must exactly match Android versionName')
matrix=(root/'docs/BUILD_MATRIX.md').read_text(encoding='utf-8')
if '| versionCode | 63 |' not in matrix: errors.append('BUILD_MATRIX versionCode stale')
if '| versionName | 63 |' not in matrix: errors.append('BUILD_MATRIX versionName stale')
if not (root/'docs/CURRENT_STATUS.md').read_text(encoding='utf-8').startswith('# Current Status — V63'): errors.append('CURRENT_STATUS is not V62')
workflows=list((root/'.github/workflows').glob('*.yml'))+list((root/'.github/workflows').glob('*.yaml'))
if len(workflows)!=1 or workflows[0].name!='android.yml': errors.append('exactly one authoritative workflow is required')
if (root/'v38work').exists(): errors.append('workspace directory v38work must not exist')
if (root/'tools/verify_project.py.tmp').exists(): errors.append('stray verify_project.py.tmp must not exist')
if any(root.rglob('*.pyc')) or any(root.rglob('__pycache__')): errors.append('compiled Python artifacts must not be shipped')
if (root/'gradle/wrapper/gradle-wrapper.jar').exists(): errors.append('fabricated gradle-wrapper.jar must not be shipped')
if 'task.builtins' not in build or 'create("java")' not in build or 'named("java")' in build: errors.append('Protobuf Kotlin DSL java builtin configuration is invalid')

ai_names = root/'app/src/main/java/com/haddadian/universalremote/ai/AiCommandNames.kt'
boundary = (root/'app/src/main/java/com/haddadian/universalremote/ai/AiCommandBoundary.kt').read_text(encoding='utf-8')
allow_list = (root/'app/src/main/java/com/haddadian/universalremote/ai/AiCommandAllowList.kt').read_text(encoding='utf-8')
if not ai_names.exists() or 'internal object AiCommandNames' not in ai_names.read_text(encoding='utf-8'):
    errors.append('AiCommandNames shared declaration is missing')
if 'AiCommandNames.allowed' not in boundary or 'AiCommandNames.allowed' not in allow_list:
    errors.append('AI allow-list consumers must use AiCommandNames.allowed')
voice_capture = (root/'app/src/main/java/com/haddadian/universalremote/VoiceCapture.kt').read_text(encoding='utf-8')
if 'cancelAndJoin()' not in voice_capture:
    errors.append('VoiceCapture.stop must await capture coroutine shutdown before releasing AudioRecord')
if 'protocolRepositoryPublicKey"' not in build: errors.append('protocolRepositoryPublicKey must be defined')
if 'orElse("")' not in build: errors.append('protocol repository key must have empty secure default')
factory=(root/'app/src/main/java/com/haddadian/universalremote/connection/protocol/ProtocolManagerFactory.kt').read_text(encoding='utf-8')
if 'Protocol repository URL is configured but its trusted public key is missing' not in factory: errors.append('remote protocol repository must fail closed without a trusted key')
manifest_path=root/'protocol-repository/manifest.json'
try:
    manifest=json.loads(manifest_path.read_text())
    ga=manifest.get('generatedAt',''); datetime.fromisoformat(ga.replace('Z','+00:00'))
except Exception as e: errors.append(f'protocol repository generatedAt invalid: {e}')
if any('REPLACED_AT_PUBLISH' in p.read_text(errors='ignore') for p in root.glob('protocol-repository/**/*') if p.is_file() and p.suffix in {'.json','.md'}): errors.append('release placeholder remains')
if 'networkSecurityConfig' not in (root/'app/src/main/AndroidManifest.xml').read_text(): errors.append('explicit network security config missing')
if not (root/'docs/V54_FULL_AUDIT_RECONCILIATION.md').exists(): errors.append('V54 audit report missing')
manifest_text=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
if 'android.permission.RECORD_AUDIO' not in manifest_text:
    errors.append('Voice feature requires RECORD_AUDIO manifest permission')
voice_ui=(root/'app/src/main/java/com/haddadian/universalremote/ui/UniversalRemoteApp.kt').read_text(encoding='utf-8')
if 'rememberLauncherForActivityResult' not in voice_ui or 'vm.startVoice()' not in voice_ui:
    errors.append('Voice UI must request microphone permission and start the real voice transport')
if 'android.permission.CHANGE_WIFI_MULTICAST_STATE' not in manifest_text:
    errors.append('SSDP multicast permission missing')
discovery_service=(root/'app/src/main/java/com/haddadian/universalremote/infra/DeviceDiscoveryService.kt').read_text(encoding='utf-8')
android_discovery=(root/'app/src/main/java/com/haddadian/universalremote/AndroidTvDiscovery.kt').read_text(encoding='utf-8')
coordinator=(root/'app/src/main/java/com/haddadian/universalremote/infra/DiscoveryCoordinator.kt').read_text(encoding='utf-8')
hardware=(root/'app/src/main/java/com/haddadian/universalremote/infra/PhoneHardwareCapabilities.kt').read_text(encoding='utf-8')
for source, label in [(discovery_service, 'SSDP discovery'), (android_discovery, 'Android TV discovery'), (coordinator, 'discovery coordinator')]:
    if 'canRunWifiDiscovery()' not in source:
        errors.append(f'{label} must gate execution on Wi-Fi availability')
if 'FEATURE_WIFI' not in hardware or 'TRANSPORT_WIFI' not in hardware:
    errors.append('central Wi-Fi hardware/network preflight is missing')
if 'hasIrEmitter()' not in hardware:
    errors.append('central IR hardware preflight is missing')

# Protocol catalog/repository coherence: only remotely distributable protocols that have
# an executable compiled binding are allowed; IR remains a built-in-only protocol.
builtin=(root/'app/src/main/java/com/haddadian/universalremote/connection/protocol/BuiltInProtocolCatalog.kt').read_text(encoding='utf-8')
bindings=(root/'app/src/main/java/com/haddadian/universalremote/connection/protocol/ProtocolBindingRegistry.kt').read_text(encoding='utf-8')
if 'dlna-upnp-control' in builtin or 'dlna-upnp-control' in bindings:
    errors.append('unimplemented DLNA/UPnP protocol must not be advertised')
if 'ProtocolIds.GENERIC_IR_TV' not in builtin or 'ProtocolIds.GENERIC_IR_TV' not in bindings:
    errors.append('generic IR must have a built-in executable binding')
if 'protocol = "ir"' in discovery_service or 'protocol = "samsung"' in discovery_service or 'protocol = "webos"' in discovery_service or 'protocol = "sony"' in discovery_service or 'protocol = "philips"' in discovery_service or 'protocol = "roku"' in discovery_service:
    errors.append('discovery must use canonical protocol package IDs')
selector=(root/'app/src/main/java/com/haddadian/universalremote/connection/protocol/ProtocolSelector.kt').read_text(encoding='utf-8')
if 'profile.manifest.id.value == "generic-ir-tv"' not in selector or 'fingerprint.platform.equals("ir", true)' not in selector:
    errors.append('generic IR must not be a fallback for arbitrary network devices')
validator=(root/'app/src/main/java/com/haddadian/universalremote/connection/protocol/ProtocolPackageValidator.kt').read_text(encoding='utf-8')
if 'declared without its capability' not in validator:
    errors.append('protocol validator must reject command/capability mismatches')
try:
    protocols=json.loads(manifest_path.read_text()).get('protocols',[])
    if len(protocols) != 6: errors.append(f'expected 6 remotely distributable protocol releases, found {len(protocols)}')
    if any(p.get('id') in {'dlna-upnp-control','generic-ir-tv'} for p in protocols):
        errors.append('IR/DLNA must not be present in remote protocol repository')
except Exception:
    pass
if 'Ready ->' in (root/'docs/CURRENT_STATUS.md').read_text() and code and code.group(1)!='53': pass
# Multi-field data classes must use named arguments at call sites. This catches the
# recurring cross-file constructor-order regression before packaging.
for source in root.glob('app/src/**/*.kt'):
    text=source.read_text(encoding='utf-8')
    if re.search(r'\bdata\s+class\s+TvFingerprint\s*\(', text):
        continue
    if re.search(r'\bTvFingerprint\s*\(\s*[^)]*[,\n][^)]*\)', text, re.S):
        # A simple structural check: every constructor call must contain an explicit named argument.
        for match in re.finditer(r'\bTvFingerprint\s*\((.*?)\)', text, re.S):
            args=match.group(1).strip()
            if args and not re.search(r'\b(?:name|manufacturer|model|platform|address|port|capabilities)\s*=', args):
                errors.append(f'positional TvFingerprint constructor call must use named arguments: {source}')
                break
if errors:
    print("PROJECT INVARIANT FAILURES:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)
print("Project invariants: OK")
