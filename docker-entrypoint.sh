#!/bin/sh
# docker-entrypoint.sh

set -e

export SILO_VERSION="$(cat resources/VERSION.txt)"

_print_header() {
    header_text="$(cat resources/BANNER.txt)

Environment:
$(env -0 | sort -z | tr '\0  ' '\n' | grep SILO | awk '{print "  ", $0}')"
    printf '%s\n' "$header_text"
}

_print_help_input() {
    help_text_input="
Error: The input directory $SILO_INPUT is empty.
Check source directory in docker-compose.yml.

Exiting."
    printf '%s\n' "$help_text_input"
}

_check_input_directory() {
    if [ -d "$SILO_INPUT" ] && files=$(ls -qAH -- "$SILO_INPUT") && [ -z "$files" ]; then
        _print_help_input
        exit 1
    fi
}

_print_header
_check_input_directory
printf '%s\n' ""
exec "$@"
