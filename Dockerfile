FROM openjdk:17-oracle
COPY moi-0.0.1-SNAPSHOT.jar /var/jenkins_home/workspace/moi/target/moi-0.0.1-SNAPSHOT.jar
WORKDIR /home/ubuntu
RUN sh -c 'touch moi-0.0.1-SNAPSHOT.jar'
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "moi-0.0.1-SNAPSHOT.jar"]
