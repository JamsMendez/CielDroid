#!/usr/bin/env python3
"""Unit tests for tools/verify_scene_assets.py (stdlib only).

Run with: python3 -m unittest discover -s tools -p 'test_*.py'
"""

import io
import os
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_scene_assets as vsa  # noqa: E402


def build_fixture(root):
    """Create a minimal repository layout with a checked-out scene submodule."""
    scenes = root / vsa.SCENES_DIR
    paths = list(vsa.SHARED)
    for scene in vsa.SCENES:
        base = scenes / scene
        (base / "js").mkdir(parents=True)
        (base / "index.html").write_text(
            '<link rel="stylesheet" href="styles.css">'
            '<script src="js/main.js"></script>'
            '<script src="../shared/js/alert-overlay.js"></script>', encoding="utf-8")
        (base / "styles.css").write_text(
            "@font-face { src: url('../shared/fonts/ArchivoBlack-Regular.ttf'); }",
            encoding="utf-8")
        (base / "js/main.js").write_text("// main\n", encoding="utf-8")
        paths += [f"scenes/{scene}/index.html", f"scenes/{scene}/styles.css",
                  f"scenes/{scene}/js/main.js"]
    for shared in vsa.SHARED:
        target = root / vsa.ASSETS_DIR / shared
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text("shared\n", encoding="utf-8")
    (scenes / "README.md").write_text("# CielScenes\n", encoding="utf-8")
    (scenes / ".git").write_text("gitdir: ../../.git/modules/scenes\n", encoding="utf-8")
    allowlist = root / vsa.ALLOWLIST
    allowlist.parent.mkdir(parents=True)
    entries = "\n".join(f'        "{p}",' for p in sorted(paths))
    allowlist.write_text(
        "object SceneAssets {\n    val paths = setOf(\n" + entries + "\n    )\n}\n"
        "object SceneAdaptations {\n"
        '    const val MINI_TRANSPARENT = "adaptations/mini-transparent.css"\n}\n',
        encoding="utf-8")
    return scenes


def run(root):
    out, err = io.StringIO(), io.StringIO()
    with redirect_stdout(out), redirect_stderr(err):
        try:
            vsa.verify(root)
            return True, out.getvalue()
        except ValueError as error:
            return False, str(error)


class VerifySceneAssetsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.scenes = build_fixture(self.root)

    def tearDown(self):
        self.tmp.cleanup()

    def test_checked_out_submodule_passes(self):
        ok, message = run(self.root)
        self.assertTrue(ok, message)
        self.assertIn("README.md", message)  # reported as present but never served

    def test_empty_submodule_directory_fails_with_init_hint(self):
        for child in sorted(self.scenes.rglob("*"), reverse=True):
            child.unlink() if child.is_file() else child.rmdir()
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("git submodule update --init", message)

    def test_missing_submodule_directory_fails_with_init_hint(self):
        for child in sorted(self.scenes.rglob("*"), reverse=True):
            child.unlink() if child.is_file() else child.rmdir()
        self.scenes.rmdir()
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("git submodule update --init", message)

    def test_missing_allowlisted_file_fails(self):
        (self.scenes / "idle/js/main.js").unlink()
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("scenes/idle/js/main.js", message)

    def test_symlink_inside_submodule_fails(self):
        os.symlink("/etc/hostname", self.scenes / "idle/js/escape.js")
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("Symlink", message)

    def test_reference_to_unallowlisted_file_fails(self):
        (self.scenes / "raphael/js/extra.js").write_text("//\n", encoding="utf-8")
        index = self.scenes / "raphael/index.html"
        index.write_text(index.read_text(encoding="utf-8")
                         + '<script src="js/extra.js"></script>', encoding="utf-8")
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("js/extra.js", message)

    def test_reference_escaping_assets_fails(self):
        index = self.scenes / "explorer/index.html"
        index.write_text('<script src="../../../../secret.js"></script>', encoding="utf-8")
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("escapes", message)

    def test_unsafe_allowlist_entry_fails(self):
        allowlist = self.root / vsa.ALLOWLIST
        allowlist.write_text(allowlist.read_text(encoding="utf-8").replace(
            '"scenes/idle/js/main.js"', '"scenes/../idle/js/main.js"'), encoding="utf-8")
        ok, message = run(self.root)
        self.assertFalse(ok)
        self.assertIn("Unsafe", message)


if __name__ == "__main__":
    unittest.main()
