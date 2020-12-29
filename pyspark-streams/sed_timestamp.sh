#!/bin/bash

for filename in old_data/*; do
  fname=$(echo ${filename} | awk -F/ '{print $NF}')
  new_fname='data/'${fname}
  echo ${new_fname}
  sed -E 's/ PM| AM//' ${filename} > ${new_fname}
done


#sed -E 's/ PM| AM//' data/1.json > data/edited.json
