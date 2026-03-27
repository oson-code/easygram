FROM imirsaburov/spring-builder:21-jdk-alpine as builder

ENV TZ="Asia/Tashkent"
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

ENV TZ="Asia/Tashkent"
ENV HOME=/app

RUN mkdir -p $HOME
WORKDIR $HOME

COPY --from=builder /app/app.jar /app/app.jar

RUN ls -la

ENTRYPOINT ["java", "-jar", "/app/app.jar" ]
