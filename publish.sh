#!/usr/bin/env bash

read -r -p  "Docker hub username: " username
read -s -r -p  "Docker hub password: " password
echo

if ! echo "$password" | docker login -u "$username" --password-stdin;
then
  echo "Docker login failed..."
  exit 1
fi

HUB_TOKEN=$(curl -s -H "Content-Type: application/json" -X POST -d '{"username": "'${username}'", "password": "'${password}'"}' https://hub.docker.com/v2/users/login/ | jq -r .token)
VERSION=$(grep -e "^version =" build.gradle | cut -d= -f2 | tr -d "'" | tr -d " ")

docker image rm -f "virtualtechgrid/slickbatch:$VERSION" "virtualtechgrid/slickbatch:latest" || exit 1

if docker manifest inspect "virtualtechgrid/slickbatch:$VERSION" > /dev/null 2> /dev/null;
then
  echo "Image version $VERSION exists..."
  read -r -p  "Do you want to force overwrite? (Y/N): " -n1 force
  echo
  if [ "$force" != "Y" ] && [ "$force" = "y" ];
  then
    curl -X DELETE \
      -H "Accept: application/json" \
      -H "Authorization: JWT $HUB_TOKEN" \
      https://hub.docker.com/v2/repositories/virtualtechgrid/slickbatch/tags/latest/
    curl -X DELETE \
          -H "Accept: application/json" \
          -H "Authorization: JWT $HUB_TOKEN" \
          "https://hub.docker.com/v2/repositories/virtualtechgrid/slickbatch/tags/$VERSION/"
  else
    echo "Not forcing..."
    exit 0
  fi
else
  echo "Image version $VERSION does not exists..."
fi

echo "Publishing..."

docker build -f Dockerfile -t "virtualtechgrid/slickbatch:$VERSION" -t "virtualtechgrid/slickbatch:latest" --build-arg "JAR_FILE=build/libs/slickbatch-${VERSION}.jar" . || exit 1

docker push "virtualtechgrid/slickbatch:$VERSION" || exit 1
docker push "virtualtechgrid/slickbatch:latest"
