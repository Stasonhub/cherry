#!/bin/sh

set -ex

lein do clean, cljsbuild once
cp package.json dist/
cp README.md dist/

cd dist/

npm publish
npm install -g .
