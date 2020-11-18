#!/bin/bash

  #
  # HELPER FUNCTIONS
  #

  # Run a coverage report for a module
  run_python2() {
    python2 -m coverage run --rcfile="${SCRIPT_DIR}/coverage.cfg" nose_tests.py False
                  # --source="src/pycompss" \
                  # --omit="/usr/lib/*" \
                  # --omit="src/pycompss/api/tests_parallel/*" \
                  # --concurrency=multiprocessing \  # not allowed as flag with the others
                  # --omit="src/pycompss/util/translators/*" \
    ev=$?
    if [ "$ev" -ne 0 ]; then
      echo "[ERROR] Coverage2 tests FAILED failed with exit value: $ev"
      exit $ev
    fi
    python2 -m coverage combine
    python2 -m coverage report -m
  }

  run_python3() {
    python3 -m coverage run -a --rcfile="${SCRIPT_DIR}/coverage.cfg" nose_tests.py False
                  # --source="src/pycompss" \
                  # --omit="/usr/lib/*" \
                  # --omit="src/pycompss/api/tests_parallel/*" \
                  # --concurrency=multiprocessing \  # not allowed as flag with the others
                  # --omit="src/pycompss/util/translators/*" \
    ev=$?
    if [ "$ev" -ne 0 ]; then
      echo "[ERROR] Coverage3 tests FAILED failed with exit value: $ev"
      exit $ev
    fi
    python3 -m coverage combine --append
    python3 -m coverage report -m
  }


  #
  # MAIN
  #
  CURRENT_DIR="$(pwd)"
  SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  # shellcheck disable=SC2164
  cd "${SCRIPT_DIR}"

  export COVERAGE_PROCESS_START=${SCRIPT_DIR}/coverage.cfg

  # Run coverage on pycompss folder
  run_python2

  # Generate XML file
  python2 -m coverage xml
  ev=$?
  if [ "$ev" -ne 0 ]; then
    echo "[ERROR] Coverage2 XML generation failed with exit value: $ev"
    exit $ev
  fi

  # Run coverage on pycompss folder
  run_python3

  # Generate XML file
  python3 -m coverage xml
  ev=$?
  if [ "$ev" -ne 0 ]; then
    echo "[ERROR] Coverage3 XML generation failed with exit value: $ev"
    exit $ev
  fi

  # shellcheck disable=SC2164
  cd "${CURRENT_DIR}"
  # Exit all ok
  exit 0
