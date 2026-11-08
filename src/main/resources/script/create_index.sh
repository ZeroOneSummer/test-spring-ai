#!/bin/bash

# ============================================
# Pinecone 向量库 Index 创建脚本
# ============================================

# --- 配置区域 ---
API_KEY="${PINECONE_API_KEY}"
INDEX_NAME="zero-idx2"
DIMENSION=1024
METRIC="cosine"
CLOUD="aws"
REGION="us-east-1"
API_VERSION="2026-04"

# --- 检查 API Key ---
if [ -z "$API_KEY" ]; then
    echo "[ERROR] PINECONE_API_KEY 环境变量未设置"
    echo "请先设置: export PINECONE_API_KEY=你的API密钥"
    exit 1
fi

echo "[INFO] 正在创建 Pinecone Index: $INDEX_NAME"
echo "       维度: $DIMENSION, 度量: $METRIC"
echo "       云: $CLOUD, 区域: $REGION"
echo

# --- 发送创建请求 ---
curl -s -X POST "https://api.pinecone.io/indexes" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Api-Key: $API_KEY" \
    -H "X-Pinecone-Api-Version: $API_VERSION" \
    -d "{
        \"name\": \"$INDEX_NAME\",
        \"dimension\": $DIMENSION,
        \"metric\": \"$METRIC\",
        \"spec\": {
            \"serverless\": {
                \"cloud\": \"$CLOUD\",
                \"region\": \"$REGION\"
            }
        }
    }"

echo
echo
echo "[INFO] 创建请求已发送, Index 初始化需要几分钟"
echo "       可通过以下命令查看状态:"
echo "       curl -s 'https://api.pinecone.io/indexes' -H 'Api-Key: \$PINECONE_API_KEY'"