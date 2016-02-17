#!/bin/sh
#

[ -d dist ] && ( set -x; rm -rf dist )
[ -d bower_components ] && ( set -x; rm -rf bower_components )
[ -d node_modules ] && ( set -x; rm -rf node_modules )
