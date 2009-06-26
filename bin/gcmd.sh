#!/bin/bash

# Executes a command at one or more Grid sites
# ssh is used to login at each site
# All occurences of the string '$SITE' in the command will be replaced by
# the site name.
#
# Options:
# -n: prevents reading from stdin
# -bg: executes each command in the background
# -wait: wait for all commands to be finished (useful in combination with -bg)
# -v: verbose output

USAGE="usage: $0 [-n] [-bg] [-wait] [-v] [-o dir] <site[:param]>+ - <command>"

. $(dirname $0)/gconfig
. $(dirname $0)/gfunctions

# read options
OPTIONS=
BACKGROUND=
WAIT=
VERBOSE=
OUTPUT_DIR=

while true; do
	case $1 in
	-help | -h | --help)
		echo $USAGE
		exit 1 ;;
	-n)
		OPTIONS="$OPTIONS -n"
		shift ;;
	-bg)
		BACKGROUND=true
		shift ;;
	-wait)
		WAIT=true
		shift ;;
	-v | -verbose)
		VERBOSE=true 
		shift ;;
	-o)
		OUTPUT_DIR=$2
		shift 2 ;;
	-*)
		echo invalid option: $1
		echo $USAGE
		exit 1 ;;
	*)
		break ;;
	esac
done

# read sites
SITES=
while [ "$1" != "-" -a -n "$1" ]; do
	SITES="$SITES $1"
	shift
done
shift

COMMAND="$*"

# sanity checks
if [ -z "$SITES" -o $# -eq 0 ]; then
   echo $USAGE;
   exit 1;
fi

if [ -n "$OUTPUT_DIR" -a ! -d "$OUTPUT_DIR" ]; then
	echo "Output directory $OUTPUT_DIR does not exist"
	echo $USAGE
	exit 1;
fi

# check if your key is already added to the SSH agent 
ssh-add -l >/dev/null
if [ $? -eq 1 ]; then
	ssh-add
fi

# execute command
PIDS=
for SITE_DESC in $SITES; do
	SITE=$(echo $SITE_DESC | sed -e 's/:.*//')
	PARAM=$(echo $SITE_DESC | sed -e 's/.*://')

	if [ $SITE = $PARAM ]; then PARAM=; fi
	

	SED_CMD="s/\$SITE/$SITE/g"
	SITE_CMD=$(echo $COMMAND | sed -e $SED_CMD)

	SED_CMD="s/\$PARAM/$PARAM/g"
	SITE_CMD=$(echo $SITE_CMD | sed -e $SED_CMD)

	if [ -n "$VERBOSE" ]; then print -style bold $SITE:~$ $SITE_CMD; fi

	if [ -n "$BACKGROUND" ]; then
		if [ -z "$OUTPUT_DIR" ]; then
			ssh $OPTIONS $SITE ". .bash_profile; $SITE_CMD" &
		else
			ssh $OPTIONS $SITE ". .bash_profile; $SITE_CMD 2>&1" >$OUTPUT_DIR/$SITE.0 &
		fi
	else
		if [ -z "$OUTPUT_DIR" ]; then
			ssh $OPTIONS $SITE ". .bash_profile; $SITE_CMD"
		else
			ssh $OPTIONS $SITE ". .bash_profile; $SITE_CMD 2>&1" >$OUTPUT_DIR/$SITE.0
		fi
	fi
	PIDS="$PIDS $!"
done

# wait for commands to finish (if needed)
if [ -n "$WAIT" ]; then
	for i in $PIDS; do 
		wait $i; 
	done
fi

