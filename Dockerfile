FROM openjdk:8-jre-alpine
WORKDIR /usr/repoxbot

ENV REPOXBOT_FILE repoXbot.jar

COPY build/libs/$REPOXBOT_FILE .

EXPOSE 8080

ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Xms16m -Xmx32m -jar $REPOXBOT_FILE"]
