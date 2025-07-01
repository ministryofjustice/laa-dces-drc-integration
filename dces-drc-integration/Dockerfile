FROM amazoncorretto:21-alpine
RUN apk update \
&& apk add curl
RUN mkdir -p /opt/laa-dces-drc-integration/
WORKDIR /opt/laa-dces-drc-integration/
COPY ./build/libs/laa-dces-drc-integration.jar /opt/laa-dces-drc-integration/app.jar
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup
USER 1001
EXPOSE 8088 8099
ENTRYPOINT ["java","-jar","app.jar"]