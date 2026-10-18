#!/usr/bin/env python3
"""Reject version/tag mismatch before publishing SDK artifacts."""
import argparse
import re
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('tag')
args = parser.parse_args()
if not re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+(?:-[a-z0-9.]+)?', args.tag):
    raise SystemExit('Use a version tag such as v0.1.0.')
version = ET.parse(root / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
if args.tag != 'v' + version:
    raise SystemExit('Tag and Maven artifact version do not match.')
if ('## ' + version) not in (root / 'CHANGELOG.md').read_text():
    raise SystemExit('Missing changelog entry for the release.')
print('Release tag, artifact version and changelog match.')
