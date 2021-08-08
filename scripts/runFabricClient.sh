if [ $(echo basename $PWD) = "scripts" ]; then
  cd ..
fi
./gradlew :platforms:fabric:runClient
