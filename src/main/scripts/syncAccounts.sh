#!/bin/bash
# This script is executed by passing in the file name containing a
# list of account ids, the user's login name, and password.
# E.g. of executing script:
# nohup sh syncAccounts.sh <accounts_file_name> <login_name> <password> &
# Script Rico Adams

FILE=$1

if [ -f $FILE ];
then
    rm  "$1.stop"
fi

java -jar sync-accounts-1.0-jar-with-dependencies.jar "filename=$FILE" "login=$2" "password=$3"