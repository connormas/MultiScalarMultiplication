#!/bin/bash

# Author: Connor Masterson
# This script tests how fast my MSM modules takes to complete a given
# workload with a different number of multipliers


# INITIAL BOOKKEEPING AND ENVIRONMENT VARIABLES
TOPLEVEL_DIR=~/Documents/Thesis/MultiScalarMultiplication
OUTFILE=parallelism-results.txt

# RUN TESTS
if [ -e $OUTFILE ]; then
    rm $OUTFILE
fi
touch $OUTFILE

echo "Starting tests, results will be in $OUTFILE"

sbt test | grep -A 1 "TopLevelTest" >> $OUTFILE
