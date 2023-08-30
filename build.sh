#!/usr/bin/env bash
set -e

docker_repo="ghcr.io/luca-heitmann/foodpollbot:1.0.0" # FIXME version needs to be changed in Dockerfile.arm64 too, because Gradle gets stuck when building the arm image directly

docker build -f Dockerfile.amd64 -t "${docker_repo}-amd64" .
docker build -f Dockerfile.arm64 --platform linux/arm64 -t "${docker_repo}-arm64" .
docker push "${docker_repo}-amd64"
docker push "${docker_repo}-arm64"
