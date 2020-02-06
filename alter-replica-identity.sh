#!/usr/bin/env bash

DRY_RUN=n
DB=_

usage() {
    echo "usage: $0 [--dry-run] DB_NAME"
    exit
}

main() {
    parse_args "$@"
    for tbl in $(psql -qAt -h localhost -d "$DB" -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public';") ; do
        echo psql -h localhost -d "$DB" -c "ALTER TABLE \"$tbl\" REPLICA IDENTITY FULL" ;
        if [[ $DRY_RUN = n ]]; then
            psql -h localhost -d "$DB" -c "ALTER TABLE \"$tbl\" REPLICA IDENTITY FULL" ;
        fi
    done
}

parse_args() {
    local arg
    while [[ $# -gt 0 ]]; do
        arg="$1" && shift
        case "$arg" in
            --dry-run)
                DRY_RUN=y
                ;;
            -h|--help)
                usage
                ;;
            *)
                if [[ $DB = _ ]]; then
                    DB="$arg"
                else
                    usage
                fi
                ;;
        esac
    done
    if [[ $DB = _ ]]; then
        echo "Missing DB_NAME"
        usage
    fi
}

main "$@"
