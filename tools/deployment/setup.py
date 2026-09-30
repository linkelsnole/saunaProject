#!/usr/bin/env python3
"""Install the independent LAB updater. Does not change live JARs or services."""
import argparse
import json
from pathlib import Path
import shlex
import subprocess

ROOT = Path(__file__).resolve().parents[2]
DESTINATION = '/opt/webpanel/sauna-updater-lab'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install-lab', action='store_true', required=True)
    parser.add_argument('--public-lab', action='store_true', help='Expose the isolated lab on server port 8087')
    args = parser.parse_args()
    options = ['-i', str(Path.home()/'.ssh/deploy_key'), '-o', 'BatchMode=yes',
               '-o', 'StrictHostKeyChecking=yes', '-o', 'ConnectTimeout=8',
               '-o', 'ServerAliveInterval=5', '-o', 'ServerAliveCountMax=2']
    host = 'webpanel@212.164.86.237'
    ssh = ['ssh', *options, host]
    files = {str(path.relative_to(ROOT)): path.read_text() for path in [
        ROOT/'tools/deploy_small_remote.py',
        ROOT/'tools/deployment/github_release.py', ROOT/'tools/deployment/updater.py',
        ROOT/'tools/deployment/lab_instance.py', ROOT/'tools/deployment/release_history.py',
    ]}
    config = dict(target='lab', root=DESTINATION, preview=True, preview_port=8087 if args.public_lab else 18082,
                  preview_address='0.0.0.0' if args.public_lab else '127.0.0.1',
                  token='/opt/webpanel/.config/sauna-updater-small/github.token')
    payload = json.dumps(dict(files=files, config=config))
    remote = r'''
import json, os, pathlib, subprocess, sys, tempfile
os.umask(0o077)
data = json.load(sys.stdin)
root = pathlib.Path('/opt/webpanel/sauna-updater-lab')
if root.is_symlink(): raise SystemExit('Unexpected symlink')
root.mkdir(mode=0o700, exist_ok=True)
for name, content in data['files'].items():
    path = root / name
    if path.is_symlink(): raise SystemExit('Unexpected script symlink')
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + '.new')
    temporary.write_text(content)
    os.replace(temporary, path)
config = root / 'config.json'
config.write_text(json.dumps(data['config'], indent=2)+'\n')
subprocess.run(['/usr/bin/python3', str(root/'tools/deployment/updater.py'), '--config', str(config)], check=True, timeout=900)
old = subprocess.run(['crontab','-l'], text=True, capture_output=True)
if old.returncode not in (0,1) or (old.returncode == 1 and 'no crontab' not in old.stderr):
    raise SystemExit('Cannot safely read existing crontab')
marker = '# sauna-updater-lab'
lines = [line for line in old.stdout.splitlines() if not line.endswith(marker)]
lines.append('* * * * * /usr/bin/python3 '+str(root/'tools/deployment/updater.py')+' --config '+str(config)+' > '+str(root/'last-check.log')+' 2>&1 '+marker)
subprocess.run(['crontab','-'], input='\n'.join(lines)+'\n', text=True, check=True)
print('Lab updater installed in '+str(root)+'. Checks every minute. Live services unchanged.')
'''
    subprocess.run(ssh + [shlex.join(['python3', '-c', remote])], input=payload, text=True, check=True, timeout=960)


if __name__ == '__main__':
    main()
