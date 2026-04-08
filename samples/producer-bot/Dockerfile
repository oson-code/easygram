FROM imirsaburov/spring-builder:21-jdk-alpine as builder

ENV HOME=/app

RUN mkdir -p $HOME
WORKDIR $HOME

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY ./ ./

RUN mvn -f $HOME/pom.xml package -DskipTests
RUN mv ./target/*.jar /app/app.jar

RUN ls -la

FROM imirsaburov/spring-runner:21-jre-alpine

ENV HOME=/app

RUN mkdir -p $HOME
WORKDIR $HOME

COPY --from=builder /app/app.jar /app/app.jar

RUN ls -la

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
