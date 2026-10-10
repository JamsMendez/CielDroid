#!/usr/bin/env python3
"""Verify the frozen T1 scene inventory without modifying files (stdlib only)."""

import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path, PurePosixPath
import re
import sys
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
DESTINATION = PurePosixPath("app/src/main/assets")
SCENES = ("processing", "explorer", "idle", "raphael")
SHARED = (
    "scenes/shared/js/alert-overlay.js",
    "scenes/shared/fonts/ArchivoBlack-Regular.ttf",
    "scenes/shared/fonts/OFL.txt",
)
CSS_URL = re.compile(r"url\(\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s)]*))\s*\)", re.I)
CSS_IMPORT = re.compile(r"@import\s+[\"']([^\"']+)[\"']", re.I)
CSS_COMMENT = re.compile(r"/\*.*?\*/", re.S)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def relative_path(value):
    require(isinstance(value, str), "Path must be a string")
    path = PurePosixPath(value)
    require(not path.is_absolute() and ".." not in path.parts
            and str(path) == value and "\\" not in value,
            f"Unsafe or noncanonical path: {value}")
    return path


def allowed_source(path):
    value = str(path)
    if value in SHARED:
        return True
    for scene in SCENES:
        base = PurePosixPath("scenes") / scene
        if path in (base / "index.html", base / "styles.css"):
            return True
        if path.is_relative_to(base / "js") and path.suffix == ".js":
            return True
    return False


def inventory(root):
    """Include unexpected files so missing manifest entries cannot go unnoticed."""
    found = set()
    for scene in (*SCENES, "shared"):
        directory = root / "scenes" / scene
        require(directory.is_dir() and not directory.is_symlink(),
                f"Missing or symlinked scene directory: {directory}")
        for path in directory.rglob("*"):
            require(not path.is_symlink(), f"Symlink not permitted: {path}")
            if path.is_file():
                found.add(path.relative_to(root).as_posix())
    return found


class HTMLReferences(HTMLParser):
    def __init__(self):
        super().__init__()
        self.references = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "script":
            require(bool(attrs.get("src")), "Inline or empty script is not part of T1")
            self.references.append(attrs["src"])
        elif tag == "link" and set(attrs.get("rel", "").lower().split()) & {
            "stylesheet", "preload"
        }:
            require(bool(attrs.get("href")), "Empty stylesheet/preload reference")
            self.references.append(attrs["href"])


def check_reference(owner, reference, assets_root, destinations):
    url = urlsplit(reference)
    require(not url.scheme and not url.netloc and bool(url.path),
            f"Nonlocal or empty dependency in {owner}: {reference}")
    require(not url.path.startswith("/"),
            f"Root-relative dependency in {owner}: {reference}")
    target = (owner.parent / unquote(url.path)).resolve()
    require(target.is_relative_to(assets_root.resolve()),
            f"Dependency escapes assets in {owner}: {reference}")
    require(target in destinations and target.is_file(),
            f"Missing/unmanifested dependency in {owner}: {reference}")


def verify(source):
    manifest = json.loads((ROOT / "docs/source-assets.json").read_text(encoding="utf-8"))
    require(manifest["schema_version"] == 1, "Unsupported manifest schema")
    files = manifest["files"]
    require(isinstance(files, list) and files, "Empty inventory")
    assets_root = ROOT / str(DESTINATION)
    source_paths = set()
    destinations = set()
    byte_count = 0
    for entry in files:
        origin = relative_path(entry["source_path"])
        destination = relative_path(entry["destination_path"])
        require(allowed_source(origin), f"Out-of-scope source: {origin}")
        require(destination == DESTINATION / origin,
                f"Dependency layout changed: {destination}")
        require(str(origin) not in source_paths, f"Duplicate source: {origin}")
        source_paths.add(str(origin))
        path = ROOT / str(destination)
        require(path.resolve().is_relative_to(assets_root.resolve())
                and not path.is_symlink(), f"Unsafe destination: {path}")
        destinations.add(path.resolve())
        data = path.read_bytes()
        require(len(data) == entry["bytes"], f"Size mismatch: {destination}")
        require(hashlib.sha256(data).hexdigest() == entry["sha256"],
                f"SHA256 mismatch: {destination}")
        byte_count += len(data)
        if source is not None:
            original = source / str(origin)
            require(not original.is_symlink(), f"Symlinked source: {origin}")
            require(original.read_bytes() == data, f"Source bytes differ: {origin}")
    required = set(SHARED)
    for scene in SCENES:
        required.update((f"scenes/{scene}/index.html", f"scenes/{scene}/styles.css"))
        require(any(p.startswith(f"scenes/{scene}/js/") for p in source_paths),
                f"Missing JavaScript tree: {scene}")
    require(required <= source_paths, "Missing scene entrypoints or shared resources")
    require(inventory(assets_root) == source_paths, "Destination inventory differs from manifest")
    if source is not None:
        require(inventory(source) == source_paths, "Source inventory differs from manifest")
    require(manifest["totals"] == {"files": len(files), "bytes": byte_count},
            "Manifest totals mismatch")

    reference_count = 0
    for path in sorted(destinations):
        references = []
        if path.suffix == ".html":
            parser = HTMLReferences()
            parser.feed(path.read_text(encoding="utf-8"))
            parser.close()
            references = parser.references
        elif path.suffix == ".css":
            text = CSS_COMMENT.sub("", path.read_text(encoding="utf-8"))
            references = [next(part for part in match.groups() if part is not None)
                          for match in CSS_URL.finditer(text)]
            references.extend(CSS_IMPORT.findall(text))
        for reference in references:
            check_reference(path, reference, assets_root, destinations)
            reference_count += 1
    comparison = "; source byte equality and inventory verified" if source else ""
    print(f"OK: {len(files)} files, {byte_count} bytes, {reference_count} local references"
          f"; SHA256 and destination inventory verified{comparison}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Read-only upstream CieLinux checkout")
    args = parser.parse_args()
    try:
        verify(args.source.resolve() if args.source else None)
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
