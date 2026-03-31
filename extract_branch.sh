#!/bin/bash
set -e

# 1. 当前要部署的 commit
CURRENT_COMMIT=$(git rev-parse HEAD)

# 2. 获取上次部署的 commit（使用 tag）
LAST_DEPLOY_TAG="last-deploy"
if git rev-parse "$LAST_DEPLOY_TAG" >/dev/null 2>&1; then
    LAST_DEPLOY_COMMIT=$(git rev-parse "$LAST_DEPLOY_TAG")
else
    echo "⚠️  Tag $LAST_DEPLOY_TAG not found, using parent of current commit (assuming first deployment)."
    LAST_DEPLOY_COMMIT=$(git rev-parse "$CURRENT_COMMIT^1" 2>/dev/null || git rev-list --max-parents=0 HEAD)
fi

echo "LAST_DEPLOY_COMMIT: $LAST_DEPLOY_COMMIT"
echo "CURRENT_COMMIT: $CURRENT_COMMIT"

# 3. 找出从上次部署到当前部署之间，最新的一次将 toko-qa 合并到 toko-master 的合并提交
#    （取最新的，即按提交时间倒序，第一个）
MERGE_QA=$(git log --merges --format="%H" "$LAST_DEPLOY_COMMIT..$CURRENT_COMMIT" --grep="Merge pull request .* from bc/toko-qa" | head -n1)

if [ -z "$MERGE_QA" ]; then
    echo "No qa → master merge commit found in this range."
    exit 0
fi

echo "Latest qa→master merge commit: $MERGE_QA"

# 4. 获取该合并提交的第二个父节点，即合并时的 toko-qa 分支 commit
QA_COMMIT=$(git rev-parse "$MERGE_QA^2")

# 5. 获取上一次部署时的 qa 分支状态（用于确定 qa 上的新增范围）
if git rev-parse "$LAST_DEPLOY_COMMIT^2" >/dev/null 2>&1; then
    LAST_QA_COMMIT=$(git rev-parse "$LAST_DEPLOY_COMMIT^2")
else
    LAST_QA_COMMIT=$(git merge-base "$LAST_DEPLOY_COMMIT" "$QA_COMMIT")
fi

echo "QA_COMMIT: $QA_COMMIT"
echo "LAST_QA_COMMIT: $LAST_QA_COMMIT"

# 6. 在 qa 分支的历史中，找出从上次部署以来所有 feature 分支合并到 qa 的 PR 提交
#    只取最新的一个（按提交时间倒序，第一个）
LATEST_FEATURE_MSG=$(git log --merges --format="%s" "$LAST_QA_COMMIT..$QA_COMMIT" | grep -E "Merge pull request .* from bc/" | head -n1)

if [ -z "$LATEST_FEATURE_MSG" ]; then
    echo "No feature branch merge found in qa within this range."
    exit 0
fi

# 7. 从最新的 feature 合并信息中提取源分支名
if [[ "$LATEST_FEATURE_MSG" =~ Merge\ pull\ request\ #[0-9]+\ from\ bc/(.+) ]]; then
    BRANCH_NAME="${BASH_REMATCH[1]}"
    echo "Latest feature branch deployed: $BRANCH_NAME"
else
    echo "Unable to parse branch name from: $LATEST_FEATURE_MSG"
    exit 1
fi