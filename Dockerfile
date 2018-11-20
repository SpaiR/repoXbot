FROM openjdk:8-jre-alpine

ENV REPOXBOT_FILE repoXbot-fat.jar
ENV REPOXBOT_HOME /usr/repoxbot

EXPOSE 8080

COPY build/libs/$REPOXBOT_FILE $REPOXBOT_HOME/

WORKDIR $REPOXBOT_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -Xms16m -Xmx32m -jar $REPOXBOT_FILE"]
