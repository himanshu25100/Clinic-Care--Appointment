#!/usr/bin/env bash
# Compiles the application and the tests into out/
set -e
rm -rf out
mkdir -p out
javac -d out $(find src test -name "*.java")
echo "Build OK -> out/"
echo "Run app  : java -cp out com.clinic.Main"
echo "Run tests: java -cp out com.clinic.test.SelfTest"
