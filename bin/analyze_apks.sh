#!/bin/bash

# uncommment and write the directory containing .apk files
#e.g. FILES=apks/*

#FILES=

for f in $FILES
do
  echo "Processing $f file..."
  #touch $f.txt
  ant -Dapk=$f -Dandroid.api.version=19 -Dpackage.name=$f > $f.txt
done
