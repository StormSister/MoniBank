#!/bin/bash

args=("$@")

for ((i = 0; i < ${#args[@]} - 1; i++)); do
  if [[ "${args[$i]}" == "-scriptport" ]]; then
    args[$((i + 1))]="${args[$((i + 1))]#localhost:}"
  fi
done

exec /usr/bin/s3270.real "${args[@]}"