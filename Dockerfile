FROM amazoncorretto:25
COPY target/*.jar app.jar
RUN yum install -y tzdata && \
    rm /etc/localtime && \
    ln -snf /usr/share/zoneinfo/Europe/Moscow /etc/localtime && \
    echo "Europe/Moscow" > /etc/timezone

VOLUME /app/data

ENTRYPOINT ["java","-jar","/app.jar"]