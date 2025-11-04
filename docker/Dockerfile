FROM amazoncorretto:21
WORKDIR /app
COPY build/libs/address-executable.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

#  1. Build the JAR locally:
#  ./gradlew build
#
#  2. Build the Docker image:
#  sudo docker build -t address:latest -f docker/Dockerfile .
#
#  3. Run it:
#  sudo docker run -p 8080:8080 address:latest
#
#  To avoid using sudo you can do sudo -i to get root privilege
#
#  Remove stopped containers:
#  sudo docker container prune
#
#  Remove unused images:
#  sudo docker image prune
#
#  Remove both at once:
#  sudo docker system prune
#
#  See what you have:
#  sudo docker images           # List images
#  sudo docker ps -a           # List all containers (including stopped)
#
