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
echo ""
echo "Now doing..."


for p in 1 2 4; do #8 16 32 64 128 256 512; do
    echo "Number of Point Multipliers: $p"
    
    sbt test | grep -A 1 "PMNaive Tests starting" >> $OUTFILE

done
