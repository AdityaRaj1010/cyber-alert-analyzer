# =====================================================================
# Cybersecurity Alert Analyzer - Render-ready Docker image (SQLite)
# =====================================================================
# Stage 1: build the WAR with Maven
# Stage 2: drop the WAR into a Tomcat 9 image
#
# SQLite needs no database server. The DB file is created automatically
# inside the container on first request. To make it persistent across
# deploys, attach a Render Disk and set DB_FILE=/var/data/cyber_alerts.db
# (free tier has ephemeral disk - data resets each redeploy, that's OK
#  for a class demo because schema + sample data auto-rebuilds).
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

# Install ourselves as ROOT so the servlets are reachable at:
#   https://<your-app>.onrender.com/login
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/CyberAlertAnalyzer.war /usr/local/tomcat/webapps/ROOT.war

# Render injects $PORT - bind Tomcat to it.
ENV PORT=8080
RUN sed -i 's/port="8080"/port="${port.http}"/' /usr/local/tomcat/conf/server.xml
CMD ["sh", "-c", "catalina.sh run -Dport.http=${PORT}"]
