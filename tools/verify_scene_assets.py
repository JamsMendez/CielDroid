#!/usr/bin/env python3
"""Verify the CielScenes submodule against the SceneAssets allowlist (stdlib only).

The scene web UI lives in the CielScenes repository, consumed as a git submodule at
app/src/main/assets/scenes; the submodule gitlink pins the exact revision. This
script never modifies files. It checks that the submodule is checked out, that every
path literally allowlisted in SceneAssets.kt exists as a regular file inside the
assets tree, that no symlink exists in the scene tree, and that every local HTML/CSS
dependency resolves inside the assets tree to an allowlisted file.
"""

from html.parser import HTMLParser
from pathlib import Path, PurePosixPath
import re
import sys
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = PurePosixPath("app/src/main/assets")
SCENES_DIR = ASSETS_DIR / "scenes"
ALLOWLIST = PurePosixPath(
    "app/src/main/java/com/jamsmendez/cieldroid/scenes/SceneAssets.kt")
SCENES = ("processing", "explorer", "idle", "raphael")
SHARED = (
    "scenes/shared/js/alert-overlay.js",
    "scenes/shared/fonts/ArchivoBlack-Regular.ttf",
    "scenes/shared/fonts/OFL.txt",
)
INIT_HINT = ("Scene submodule is not checked out at {path}; run: "
             "git submodule update --init app/src/main/assets/scenes")
SCENE_ASSETS_BLOCK = re.compile(r"object\s+SceneAssets\s*\{(.*?)\n\}", re.S)
KOTLIN_STRING = re.compile(r'"([^"\\]*)"')
CSS_URL = re.compile(r"url\(\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s)]*))\s*\)", re.I)
CSS_IMPORT = re.compile(r"@import\s+[\"']([^\"']+)[\"']", re.I)
CSS_COMMENT = re.compile(r"/\*.*?\*/", re.S)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def allowlist(root):
    """Return the literal asset paths from the SceneAssets object."""
    source = (root / str(ALLOWLIST)).read_text(encoding="utf-8")
    block = SCENE_ASSETS_BLOCK.search(source)
    require(block is not None, f"SceneAssets object not found in {ALLOWLIST}")
    paths = KOTLIN_STRING.findall(block.group(1))
    require(paths, "Empty SceneAssets allowlist")
    require(len(paths) == len(set(paths)), "Duplicate SceneAssets allowlist entry")
    for value in paths:
        path = PurePosixPath(value)
        require(not path.is_absolute() and ".." not in path.parts and "\\" not in value
                and str(path) == value and path.parts[0] == "scenes",
                f"Unsafe or noncanonical allowlist path: {value}")
    return set(paths)


def scene_tree(scenes):
    """Return the files of the checked-out submodule, rejecting symlinks."""
    require(scenes.exists() and not scenes.is_symlink() and scenes.is_dir(),
            INIT_HINT.format(path=scenes))
    files = set()
    for path in scenes.rglob("*"):
        relative = path.relative_to(scenes)
        if relative.parts[0] == ".git":
            continue  # submodule gitfile / gitdir, never packaged (dotfile)
        require(not path.is_symlink(), f"Symlink not permitted: {path}")
        if path.is_file():
            files.add(PurePosixPath("scenes", *relative.parts).as_posix())
    require(files, INIT_HINT.format(path=scenes))
    return files


class HTMLReferences(HTMLParser):
    def __init__(self):
        super().__init__()
        self.references = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "script":
            require(bool(attrs.get("src")), "Inline or empty script is not permitted")
            self.references.append(attrs["src"])
        elif tag == "link" and set(attrs.get("rel", "").lower().split()) & {
            "stylesheet", "preload"
        }:
            require(bool(attrs.get("href")), "Empty stylesheet/preload reference")
            self.references.append(attrs["href"])


def references(path):
    if path.suffix == ".html":
        parser = HTMLReferences()
        parser.feed(path.read_text(encoding="utf-8"))
        parser.close()
        return parser.references
    if path.suffix == ".css":
        text = CSS_COMMENT.sub("", path.read_text(encoding="utf-8"))
        found = [next(part for part in match.groups() if part is not None)
                 for match in CSS_URL.finditer(text)]
        return found + CSS_IMPORT.findall(text)
    return []


def check_reference(owner, reference, assets_root, allowed):
    url = urlsplit(reference)
    require(not url.scheme and not url.netloc and bool(url.path),
            f"Nonlocal or empty dependency in {owner}: {reference}")
    require(not url.path.startswith("/"),
            f"Root-relative dependency in {owner}: {reference}")
    target = (owner.parent / unquote(url.path)).resolve()
    require(target.is_relative_to(assets_root),
            f"Dependency escapes assets in {owner}: {reference}")
    require(target in allowed and target.is_file(),
            f"Dependency is missing or not in SceneAssets allowlist in {owner}: {reference}")


def verify(root=ROOT):
    root = Path(root)
    assets_root = (root / str(ASSETS_DIR)).resolve()
    paths = allowlist(root)
    present = scene_tree(root / str(SCENES_DIR))

    required = set(SHARED)
    for scene in SCENES:
        required.update((f"scenes/{scene}/index.html", f"scenes/{scene}/styles.css"))
    missing_required = sorted(required - paths)
    require(not missing_required,
            f"Scene entrypoints/shared resources missing from allowlist: {missing_required}")
    missing = sorted(paths - present)
    require(not missing, f"Allowlisted files missing from scene submodule: {missing}")

    allowed = set()
    for value in paths:
        path = root / str(ASSETS_DIR) / value
        require(path.is_file() and path.resolve().is_relative_to(assets_root),
                f"Unsafe allowlisted asset: {value}")
        allowed.add(path.resolve())

    reference_count = 0
    for path in sorted(allowed):
        for reference in references(path):
            check_reference(path, reference, assets_root, allowed)
            reference_count += 1

    unserved = sorted(present - paths)
    print(f"OK: {len(paths)} allowlisted files present, {reference_count} local references "
          f"resolved; no symlinks in submodule")
    if unserved:
        print(f"NOTE: {len(unserved)} submodule file(s) not in SceneAssets allowlist "
              f"(packaged but never served): {', '.join(unserved)}")


def main():
    try:
        verify()
    except (OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
