#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail
readonly repo="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

function main {
  cd "$repo"

  wait_for_postgres
  use_correct_jvm_version
  cd "$repo"
  mvn install -Dmaven.test.skip=true
  cd "$repo/kayttooikeus-service"
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -Dserver.port=8101"
}

function wait_for_postgres {
  while ! docker compose port database 5432 >&2; do
    echo "Waiting for database to start..."
    sleep 1
  done
}

function use_correct_jvm_version {
  JAVA_HOME="$( /usr/libexec/java_home -v "21" )"
  export JAVA_HOME
}

main "$@"
