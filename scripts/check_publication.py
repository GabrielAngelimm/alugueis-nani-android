"""Check tracked/staged files and optional Git history before public publication.

Standard library only. Findings print locations and categories, never credential values.
This is a conservative local check, not a replacement for GitHub secret scanning.
"""
from __future__ import annotations

import argparse
from pathlib import PurePosixPath
import re
import subprocess
import sys

PRIVATE_DIRS = {'.codex', '.agents', '.impeccable', '.gradle', '.kotlin', '.idea',
                'design-review', 'Documentacao', 'video', 'dist', 'build',
                'audit_db_extract', 'runtime_db_extract_2', 'runtime_db_extract_3'}
PRIVATE_SUFFIXES = {'.db', '.sqlite', '.sqlite3', '.nani', '.tar', '.zip', '.apk',
                    '.aab', '.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.hprof', '.log', '.pdf', '.mp4'}
SECRET_PATTERNS = {
    'private key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----'),
    'GitHub credential': re.compile(rb'\bgh[pousr]_[A-Za-z0-9]{30,}\b|\bgithub_pat_[A-Za-z0-9_]{40,}\b'),
    'AWS access key': re.compile(rb'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b'),
    'Google API key': re.compile(rb'\bAIza[A-Za-z0-9_-]{35}\b'),
    'Slack token': re.compile(rb'\bxox[baprs]-[A-Za-z0-9-]{20,}\b'),
    'personal machine path': re.compile(rb'(?i)\b[A-Z]:[\\/]+Users[\\/]+[^\s<>"\']+'),
}


def git(*args: str) -> bytes:
    return subprocess.check_output(['git', *args], stderr=subprocess.PIPE)


def forbidden_path(name: str) -> bool:
    path = PurePosixPath(name)
    return (any(part in PRIVATE_DIRS for part in path.parts)
            or path.name == 'local.properties' or path.name == 'google-services.json'
            or name == 'brand/nani-launcher.png'
            or path.name == '.env' or path.name.startswith('.env.') and path.name != '.env.example'
            or 'keystore' in path.name.lower() and path.suffix == '.properties'
            or path.suffix.lower() in PRIVATE_SUFFIXES
            or re.search(r'\.(?:db|sqlite)(?:-|$)', path.name) is not None
            or len(path.parts) == 1 and path.suffix.lower() in {'.png', '.xml', '.csv'})


def scan(name: str, data: bytes, location: str) -> list[str]:
    findings = [f'{location}:{name}: private/local file'] if forbidden_path(name) else []
    for category, pattern in SECRET_PATTERNS.items():
        match = pattern.search(data)
        if match:
            line = data[:match.start()].count(b'\n') + 1
            findings.append(f'{location}:{name}:{line}: {category}')
    return findings


def self_test() -> None:
    assert forbidden_path('local.properties')
    assert forbidden_path('runtime_db_extract_2/databases/rental_validator_db')
    assert forbidden_path('.github/secret.keystore')
    assert not forbidden_path('app/schemas/database/4.json')
    assert not forbidden_path('docs/screenshots/overview.png')
    assert not forbidden_path('gradle/wrapper/gradle-wrapper.jar')
    fake = ('gh' + 'p_' + 'a' * 36).encode()
    assert any('GitHub credential' in item for item in scan('example.txt', fake, 'test'))
    assert scan('example.kt', b'val name = "Example"', 'test') == []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--staged', action='store_true', help='Check the exact Git index instead of HEAD.')
    parser.add_argument('--history', action='store_true', help='Also scan every reachable historical blob.')
    parser.add_argument('--self-test', action='store_true', help='Exercise the detector without accessing project files.')
    args = parser.parse_args()
    self_test()
    if args.self_test:
        print('Publication checker self-test passed.')
        return 0
    findings: list[str] = []
    scanned = 0
    if args.staged:
        names = git('ls-files', '-z').decode('utf-8').split('\0')
        for name in filter(None, names):
            findings += scan(name, git('show', ':' + name), 'index')
            scanned += 1
    else:
        entries = git('ls-tree', '-r', '-z', 'HEAD').decode('utf-8').split('\0')
        for entry in filter(None, entries):
            metadata, name = entry.split('\t', 1)
            _, kind, sha = metadata.split()
            if kind == 'blob':
                findings += scan(name, git('cat-file', 'blob', sha), 'HEAD')
                scanned += 1
    if args.history:
        for item in git('rev-list', '--objects', '--all').decode('utf-8').splitlines():
            sha, separator, name = item.partition(' ')
            if separator and git('cat-file', '-t', sha).strip() == b'blob':
                findings += scan(name, git('cat-file', 'blob', sha), 'history/' + sha[:12])
                scanned += 1
    if findings:
        print('\n'.join(sorted(set(findings))), file=sys.stderr)
        print('Publication check failed. Review the listed paths before pushing.', file=sys.stderr)
        return 1
    print(f'Publication check passed: {scanned} file/blob checks; no blocked paths or known credential patterns.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
