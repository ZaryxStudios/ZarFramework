#!/usr/bin/env python3
import sys
import os
import shutil
import re

def normalize_java(source):
    # Replace tabs with 4 spaces
    s = source.replace('\t', '    ')
    # Remove trailing whitespace
    s = re.sub(r'[ \t]+\r?\n', '\n', s)
    # Collapse more than two consecutive blank lines to two
    s = re.sub(r'\n{3,}', '\n\n', s)
    # Ensure single space after commas
    s = re.sub(r',\s*', ', ', s)
    # Ensure newline at end of file
    if not s.endswith('\n'):
        s += '\n'
    return s


def backup_file(path):
    bak = path + '.bak'
    if not os.path.exists(bak):
        shutil.copy2(path, bak)


def rewrite_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        src = f.read()
    new = normalize_java(src)
    if new != src:
        backup_file(path)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new)
        return True
    return False


def walk_and_rewrite(root):
    rewritten = 0
    total = 0
    for dirpath, dirs, files in os.walk(root):
        for name in files:
            if name.endswith('.java'):
                total += 1
                path = os.path.join(dirpath, name)
                try:
                    if rewrite_file(path):
                        rewritten += 1
                except Exception as e:
                    print(f'ERROR rewriting {path}: {e}')
    print(f'Rewrite complete: {rewritten}/{total} files modified')
    return rewritten, total


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: rewrite_java.py <root-dir>')
        sys.exit(2)
    root = sys.argv[1]
    if not os.path.isdir(root):
        print('Directory not found:', root)
        sys.exit(2)
    walk_and_rewrite(root)
