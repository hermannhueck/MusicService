#!/bin/sh
#

[ -f package.json ] && {

	which npm >/dev/null 2>&1 || {
		echo "npm not found" >&2
		echo "Missing node module: npm" >&2
		exit 1
	}

	( set -x; npm install ) status=$?; echo "===> npm status = $status"
}

[ -f bower.json ] && {

	which bower >/dev/null 2>&1 || {
		echo "bower not found" >&2
		echo "Missing node module: bower" >&2
		echo "Install with: sudo install -g bower" >&2
		exit 1
	}

	( set -x; bower install ) status=$?; echo "===> bower status = $status"
}
