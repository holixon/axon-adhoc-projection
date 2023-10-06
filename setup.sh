#!/usr/bin/env bash

GITHUB_ORGANIZATION=holunda-io
GITHUB_REPOSITORY=holunda-extension-template
BASE_GROUP_ID=io.holunda.template
BASE_ARTIFACT_ID=$GITHUB_REPOSITORY

for file in `find . \( -name "pom.xml" -o -name "*.kt" -o -name "README.md" \) -type f`
do
  sed -i '' "s/GITHUB_ORGANIZATION/${GITHUB_ORGANIZATION}/g" $file
  sed -i '' "s/GITHUB_REPOSITORY/${GITHUB_REPOSITORY}/g" $file
  sed -i '' "s/BASE_GROUP_ID/${BASE_GROUP_ID}/g" $file
  sed -i '' "s/BASE_ARTIFACT_ID/${BASE_ARTIFACT_ID}/g" $file
done

