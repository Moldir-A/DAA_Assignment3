#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn clean verify
java -Xmx2g -Xss256k -jar target/city-network-1.0.jar benchmark --full
python3 scripts/analyze.py
python3 scripts/build_report.py
java -jar target/city-network-1.0.jar demo > results/demo_output.txt
