#!/usr/bin/env bash

set -euo pipefail

usage() {
    echo "usage: $0"
    exit "$1"
}

BUILD_TIME="$(date +%Y%m%d_%H%M%S)"

main() {
    parse_args "$@"
    runv mvn package
    runv docker build . \
        -t materialize/tb:latest \
        -t materialize/tb:"$BUILD_TIME"
}

parse_args() {
    local arg
    while [[ $# -gt 0 ]]; do
        arg="$1" && shift
        case "$arg" in
            -h|--help)
                usage 0
                ;;
            *)
                echo "ERROR: unknown argument: '$arg'"
                usage 1
                ;;
        esac
    done
}

runv() {
    echo "🚀$ $*"
    "$@"
}

main "$@"
