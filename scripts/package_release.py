#!/usr/bin/env python3
"""Package this SDK into a portable Maven repository; third-party jars resolve from Central."""
from pathlib import Path
import hashlib
import shutil
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
pom = ET.parse(ROOT / 'pom.xml').getroot()
version = pom.find('m:version', NS).text
name = 'mcp-mobile-client-' + version
out = ROOT / 'target/release'
out.mkdir(parents=True, exist_ok=True)
artifacts = [(ROOT / 'target' / (name + '.jar'), name + '.jar'),
             (ROOT / 'target' / (name + '-sources.jar'), name + '-sources.jar'),
             (ROOT / 'target' / (name + '-javadoc.jar'), name + '-javadoc.jar'),
             (ROOT / 'pom.xml', name + '.pom')]
repo_path = 'repository/io/github/mcp-runtime/mcp-mobile-client/' + version + '/'
with zipfile.ZipFile(out / (name + '-repository.zip'), 'w', compression=zipfile.ZIP_DEFLATED) as bundle:
    for source, filename in artifacts:
        if not source.is_file():
            raise SystemExit('Run mvn verify before packaging: ' + filename)
        shutil.copyfile(source, out / filename)
        bundle.write(source, repo_path + filename)
    for filename in ['LICENSE', 'THIRD_PARTY.md', 'README.md']:
        bundle.write(ROOT / filename, filename)
checksums = []
for file in sorted(out.iterdir()):
    if file.name == 'SHA256SUMS':
        continue
    checksums.append(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name)
(out / 'SHA256SUMS').write_text('\n'.join(checksums) + '\n')
print('SDK release assets ready in target/release (no credentials or runtime dependencies bundled).')
