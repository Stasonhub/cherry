#!/bin/sh

set -e -x

# Release to npm

# lein do cljsbuild clean, cljsbuild once
cp package.json dist/

cd dist/

npm install -g
