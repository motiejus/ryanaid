#!/bin/sh
export WHOAMI=`basename "$0"`
HERE=`pwd`
ACTIONBASEDIR="$HERE/actiondir"
DATADIR="$HERE/datadir"
NUMBER="$1"

function usage() {
    echo -n "Enter one numeric agument between 1 and "
    wc -l < tunnel_hosts.txt
    exit 1
}

function log() {
	echo `date "+%F+%T" | tr -d '\n'` $WHOAMI[$$] "$1" >> $LOGFILE
}

function fail() {
	if [ -n "$1" ]; then
		log "----------------------------------------------------------"\
		"$1" \
			"----------------------------------------------------------"
	fi

	if [ -n "$DEST" ]; then # Adding the current dest for processing
		./next_dest "$DEST"
	fi

	if [ -f "$AUTOSSH_PIDFILE" ]; then
		log "Attempting to kill $AUTOSSH_PIDFILE"
		kill `cat $AUTOSSH_PIDFILE`
	fi

	echo "$1" >> $ACTIONBASEDIR/fail-$NUMBER.log
	if [ -n "$FBASE" ]; then
		touch "$FBASE.fail"
	fi
	exit 1
}

function proc_params() {
	[[ -n "$(echo "$NUMBER" | grep -E "^[0-9]+$")" ]] || usage

	SOX="$(sed -n "${NUMBER}p" tunnel_hosts.txt)"
	[[ -n "$SOX" ]] || usage

	export LOGFILE="$ACTIONBASEDIR/app-$NUMBER.log"

	export HOST="$(echo $SOX | cut -d: -f1)"
	export PORT="$(echo $SOX | cut -d: -f2)"

	export SPORT=$((10000+$NUMBER)) # Socks port
	export CPORT=$((22000+$NUMBER*29-5)) # Check port
}

function check_if_sport_used() {
	if [ -n "`lsof -i TCP:${SPORT}`" ]; then
		fail "Port ${SPORT} is busy:\n`lsof -i TCP:${SPORT}`"
	fi
}

function start_autossh() {
	export AUTOSSH_LOGFILE=$LOGFILE
	export AUTOSSH_PIDFILE=$ACTIONBASEDIR/autossh-$NUMBER.pid

	autossh -M${CPORT} -f -ND${SPORT} -i${HERE}/crawler_key -p${PORT} ${HOST}
	RETCODE=$?
	if [ "$RETCODE" != "0" ]; then
		fail "General autossh fail (ret code = $RETCODE).\n"\
			"Please have a look at $AUTOSSH_LOGFILE"
	fi
}

function check_autossh_pid() {
	if [ ! -f "$AUTOSSH_PIDFILE" ]; then
		fail "Autossh pidfile '${AUTOSSH_PIDFILE}' not found"
	fi
}

function check_if_display_ok() {
	if [ -z "$DISPLAY" ]; then
		fail "Bad display: $DISPLAY"
	fi
}


[[ "$#" = 1 ]] || usage

trap fail SIGINT SIGTERM

proc_params
check_if_sport_used
check_if_display_ok
start_autossh

while true; do
	check_autossh_pid

	export DEST="$(./next_dest)"
	TIME="$(date '+%F+%T')"
	DATEHOUR=$(echo $TIME | perl -pe 's/(.*)\+(\d+).*/\1_\2/') # 2010-01-01_13

	mkdir -p "${DATADIR}/${DATEHOUR}"
	export FBASE="${DATADIR}/${DATEHOUR}/${DEST}_${TIME}-${NUMBER}"
	touch "${FBASE}.started"

	from="`echo $DEST | cut -d- -f1`"
	to="`echo $DEST | cut -d- -f2`"
	check_days=180
	socks_host="127.0.0.1"
	socks_port="$SPORT"
	export from to check_days socks_host socks_port DISPLAY

	java -jar ryanaid.jar > "$FBASE.flights" 2> "$FBASE.inf"

	RET=$?
	if [ "$RET" = "0" ]; then # all fine
		rm "${FBASE}.started"
	else
		touch "${FBASE}.fail"
		./next_dest "${DEST}"
	fi
	export FBASE=

	sleep 5
done
