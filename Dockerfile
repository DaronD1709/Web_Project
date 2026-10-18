# Dong goi Nong Viet thanh 1 image: Tomcat 10.1 + WAR (chay o goc "/"). Build tu thu muc goc cua repo:
#   docker build -t nongviet .
#   docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... -e ADMIN_PASSWORD=... nongviet
# Cac bien moi truong xem docs/deploy-guide.md.

# ---- buoc 1: build file WAR ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -q -f backend/pom.xml clean package -DskipTests

# ---- buoc 2: chay tren Tomcat ----
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /src/backend/target/ecommerce.war /usr/local/tomcat/webapps/ROOT.war

# Sau reverse proxy (Render/Railway/Cloud Run): tin X-Forwarded-Proto/For de biet khach dung https va IP that cua khach.
RUN sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" protocolHeader="X-Forwarded-Proto" remoteIpHeader="X-Forwarded-For" internalProxies=".*" />\n      </Host>#' /usr/local/tomcat/conf/server.xml

# Anh san pham do Admin tai len: gan volume vao /data/uploads de khong mat khi khoi dong lai.
ENV UPLOAD_DIR=/data/uploads
RUN mkdir -p /data/uploads
ENV CATALINA_OPTS="-XX:MaxRAMPercentage=70 -Dfile.encoding=UTF-8"

COPY deploy/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 8080
CMD ["/entrypoint.sh"]
