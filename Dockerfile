# =====================================================================
# Cybersecurity Alert Analyzer - Render-ready Docker image (SQLite)
# =====================================================================
# Stage 1: build the WAR with Maven
# Stage 2: drop the WAR into a Tomcat 9 image with a minimal,
#          Render-friendly server.xml (only HTTP on $PORT, no shutdown
#          port, no AJP, no HTTPS - Render terminates TLS at the edge).
# =====================================================================

# ---- Stage 1 : build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src       ./src
COPY webapp    ./webapp
RUN mvn -B clean package -DskipTests

# ---- Stage 2 : run on Tomcat 9 ----
FROM tomcat:9.0-jdk17-temurin

# Replace the default ROOT app with ours
RUN rm -rf /usr/local/tomcat/webapps/ROOT \
        /usr/local/tomcat/webapps/docs \
        /usr/local/tomcat/webapps/examples \
        /usr/local/tomcat/webapps/host-manager \
        /usr/local/tomcat/webapps/manager
COPY --from=build /app/target/CyberAlertAnalyzer.war /usr/local/tomcat/webapps/ROOT.war

# Use the trimmed Render-friendly server.xml
COPY render-server.xml /usr/local/tomcat/conf/server.xml

# SQLite DB lives in a writable folder. On Render free tier this is
# ephemeral; on a paid plan attach a Disk and override DB_FILE.
RUN mkdir -p /var/data && chmod 777 /var/data
ENV DB_FILE=/var/data/cyber_alerts.db

# Render injects $PORT at runtime - substitute it into server.xml
# at container start, then exec Tomcat.
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "sed -i \"s/__PORT__/${PORT}/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
