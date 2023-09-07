#!/usr/bin/env bash
set -e

docker_repo="lucaheitmann/foodpollbot:1.0.1"

docker build -f Dockerfile.amd64 -t "${docker_repo}-amd64" .
docker push "${docker_repo}-amd64"

docker build -f Dockerfile.arm64 --platform linux/arm64 --build-arg baseImage="$docker_repo-amd64" -t "${docker_repo}-arm64" .
docker push "${docker_repo}-arm64"
