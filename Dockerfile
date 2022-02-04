FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/AlTech97/AuctionP2P.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/AuctionP2P /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=1 /app/target/AuctionP2P-1.0-jar-with-dependencies.jar /app

CMD /usr/bin/java -jar AuctionP2P-1.0-jar-with-dependencies.jar -m $MASTERIP -id $ID