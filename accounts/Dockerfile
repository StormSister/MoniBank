#Start with a basic image contanig Java runtime
FROM openjdk:21-jdk-slim

#Information around who maintains the image
LABEL "org.opencontainers.image.authors"="monika.com"

#Add the application jar to the image
COPY target/accounts-0.0.1-SNAPSHOT.jar accounts-0.0.1-SNAPSHOT.jar

#Execute the application
ENTRYPOINT ["java","-jar","accounts-0.0.1-SNAPSHOT.jar"]