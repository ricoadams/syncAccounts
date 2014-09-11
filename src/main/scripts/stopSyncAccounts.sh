#!/bin/bash
# This script is executed by passing in the file name containing a
# list of account ids.
# E.g. of executing script:
# sh stopSyncAccounts.sh <accounts_file_name>
# Script Rico Adams
FILE="$1.stop"
if [ -f $FILE ];
then
    rm $FILE
fi
touch $FILE