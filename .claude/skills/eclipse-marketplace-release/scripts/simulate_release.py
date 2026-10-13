#!/usr/bin/env python3
"""Run the shell steps of a GitHub Actions release job locally, as if a tag was pushed.

The repository is copied (tracked and unignored files only) to a scratch directory, so the working
tree is never modified. `uses:` steps are skipped, `${{ secrets.* }}` expand to empty strings (an
unsigned build), `${{ inputs.version }}` to the version, and steps guarded by `if: failure()` are
skipped. Variables written to $GITHUB_ENV are visible to later steps, like on GitHub.

Usage (from the repository root):
    python3 simulate_release.py [--tag v9.9.9] [--workflow .github/workflows/release.yml]
                                [--job build] [--skip-tests] [--keep]

Requires PyYAML (pip install pyyaml) and the tools the workflow calls (mvn, java, ...).
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

try:
    import yaml
except ImportError:
    sys.exit("PyYAML is required: pip install pyyaml")


def copy_repo(dest):
    files = subprocess.run(["git", "ls-files", "-co", "--exclude-standard", "-z"],
                           check=True, capture_output=True).stdout.decode().split("\0")
    for rel in filter(None, files):
        if os.path.isfile(rel):
            target = os.path.join(dest, rel)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copy2(rel, target)


def expand(value, version):
    """Expands the few expressions the release steps use."""
    def repl(match):
        expr = match.group(1).strip()
        if expr.startswith("secrets."):
            return ""
        if expr in ("inputs.version", "github.event.inputs.version"):
            return version
        if expr == "env.VERSION":
            return version
        return ""
    return re.sub(r"\$\{\{(.*?)\}\}", repl, str(value))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--tag", default="v9.9.9")
    parser.add_argument("--workflow", default=".github/workflows/release.yml")
    parser.add_argument("--job", default="build")
    parser.add_argument("--skip-tests", action="store_true", help="add -DskipTests to mvn verify")
    parser.add_argument("--keep", action="store_true", help="keep the scratch directory")
    args = parser.parse_args()

    with open(args.workflow) as f:
        workflow = yaml.safe_load(f)
    steps = workflow["jobs"][args.job]["steps"]
    version = args.tag[1:] if args.tag.startswith("v") else args.tag

    scratch = tempfile.mkdtemp(prefix="release-sim-")
    work = os.path.join(scratch, "repo")
    runner_temp = os.path.join(scratch, "runner")
    os.makedirs(runner_temp)
    copy_repo(work)
    github_env = os.path.join(scratch, "github_env")
    open(github_env, "w").close()

    env = dict(os.environ, GITHUB_REF_TYPE="tag", GITHUB_REF_NAME=args.tag,
               GITHUB_ENV=github_env, RUNNER_TEMP=runner_temp, CI="true")
    has_xvfb = shutil.which("xvfb-run") is not None
    failed = False
    try:
        for step in steps:
            name = step.get("name", step.get("uses", "?"))
            condition = str(step.get("if", ""))
            if "run" not in step:
                print(f"-- skip (uses): {name}")
                continue
            if "failure()" in condition or (failed and "always()" not in condition):
                print(f"-- skip ({condition or 'after failure'}): {name}")
                continue
            script = step["run"]
            if not has_xvfb:
                script = script.replace("xvfb-run -a ", "")
            if args.skip_tests:
                script = re.sub(r"(mvnw?\b[^\n]*\bverify\b)", r"\1 -DskipTests", script)
            step_env = dict(env)
            for key, value in (step.get("env") or {}).items():
                step_env[key] = expand(value, version)
            # values written to $GITHUB_ENV by earlier steps
            with open(github_env) as f:
                for line in f:
                    if "=" in line:
                        key, value = line.rstrip("\n").split("=", 1)
                        step_env[key] = value
            print(f"== {name}", flush=True)
            result = subprocess.run(["bash", "-eo", "pipefail", "-c", script], cwd=work, env=step_env)
            if result.returncode != 0:
                print(f"!! step failed with exit code {result.returncode}: {name}")
                failed = True

        print("\n== Result")
        site = os.path.join(work, "_site")
        if os.path.isdir(site):
            for root, _, files in os.walk(site):
                depth = root[len(site):].count(os.sep)
                if depth <= 1:
                    for file in sorted(files):
                        print("  _site" + root[len(site):] + "/" + file)
        for file in sorted(os.listdir(work)):
            if file.endswith(".zip"):
                print("  " + file)
        print(f"\nscratch directory: {scratch}" + ("" if args.keep else " (deleted)"))
        return 1 if failed else 0
    finally:
        if not args.keep:
            shutil.rmtree(scratch, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
