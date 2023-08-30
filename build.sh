#!/usr/bin/env bash
set -e

docker_repo="lucaheitmann/foodpollbot:1.0.0"

docker build -t "$docker_repo" .
docker push "$docker_repo"
