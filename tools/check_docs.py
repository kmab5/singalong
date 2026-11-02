"""Check repository Markdown links without crawling external services or private feedback."""
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]


def main():
    files = set(subprocess.check_output(['git', 'ls-files', '*.md'], cwd=ROOT, text=True).splitlines())
    files.update(str(p.relative_to(ROOT)).replace('\\', '/') for p in ROOT.glob('*.md'))
    files.update(str(p.relative_to(ROOT)).replace('\\', '/') for p in (ROOT / 'docs').rglob('*.md'))
    errors = []
    for name in sorted(files):
        path = ROOT / name
        if not path.exists(): continue
        body = re.sub(r'```.*?```', '', path.read_text(encoding='utf-8'), flags=re.S)
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', body):
            target = target.strip('<>').split('#', 1)[0]
            if not target or re.match(r'[a-zA-Z][\w+.-]*:', target): continue
            target = unquote(target)
            resolved = (ROOT / target.lstrip('/')) if target.startswith('/') else path.parent / target
            if not resolved.exists(): errors.append(f'{name}: missing {target}')
    if errors: raise SystemExit('\n'.join(errors))
    print(f'Checked local links in {len(files)} Markdown documents.')


if __name__ == '__main__': main()
