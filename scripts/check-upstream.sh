#!/usr/bin/env bash
# Check a prospective upstream merge in a disposable clone; never touch the caller's index/worktree.
set -euo pipefail
repo_root=$(git rev-parse --show-toplevel)
base_ref=${1:-HEAD}
upstream_ref=${2:-upstream/master}
base_sha=$(git rev-parse --verify "${base_ref}^{commit}") || { printf 'Invalid base reference\n' >&2; exit 1; }
upstream_sha=$(git rev-parse --verify "${upstream_ref}^{commit}") || { printf 'Invalid upstream reference\n' >&2; exit 1; }
printf 'base=%s\nupstream=%s\n' "$base_sha" "$upstream_sha"
if git merge-base --is-ancestor "$upstream_sha" "$base_sha"; then
  printf 'changed=false\nconflict=false\n'
  exit 0
fi
printf 'changed=true\n'
check_dir=$(mktemp -d "${TMPDIR:-/tmp}/swarmpit-upstream.XXXXXX")
trap 'rm -rf "$check_dir"' EXIT
git clone --quiet --shared --no-checkout -- "$repo_root" "$check_dir"
git -C "$check_dir" checkout --quiet --detach "$base_sha"
if git -C "$check_dir" -c user.name='Upstream check' -c user.email='check@example.invalid' merge --no-commit --no-ff "$upstream_sha" >"$check_dir/merge-check.log" 2>&1; then
  printf 'conflict=false\n'
else
  if [ -n "$(git -C "$check_dir" ls-files --unmerged)" ]; then
    printf 'conflict=true\n'
    git -C "$check_dir" diff --name-only --diff-filter=U
    exit 2
  fi
  cat "$check_dir/merge-check.log" >&2
  exit 1
fi
ancestor=$(git merge-base "$base_sha" "$upstream_sha")
printf '\nContract and integration changes to inspect:\n'
git diff --name-status "$ancestor" "$upstream_sha" -- src/cljc/swarmpit/routes.cljc src/cljc/swarmpit/routes_spec.cljc src/cljc/swarmpit/token.cljc src/clj/swarmpit/authentication.clj src/clj/swarmpit/authorization.clj src/clj/swarmpit/handler.clj src/clj/swarmpit/api.clj src/clj/swarmpit/event src/clj/swarmpit/docker src/cljs resources project.clj Dockerfile .github/workflows
