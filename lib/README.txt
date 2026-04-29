Manual JAR setup (only needed if you compile WITHOUT Maven).

1) sqlite-jdbc-3.45.3.0.jar
   https://github.com/xerial/sqlite-jdbc/releases
   (any 3.45.x.x build is fine)

2) servlet-api.jar  (only required to compile the servlet classes
   outside Tomcat - Tomcat's lib/ already contains it)
   Comes with any Tomcat 9 installation:
       <tomcat-home>/lib/servlet-api.jar

Compile (Windows):
    javac -d out -cp "lib\sqlite-jdbc-3.45.3.0.jar;lib\servlet-api.jar" ^
          src\model\*.java src\util\*.java src\db\*.java src\dao\*.java ^
          src\ui\*.java src\servlet\*.java src\Main.java

Run desktop GUI:
    java -cp "out;lib\sqlite-jdbc-3.45.3.0.jar" Main

Note: SQLite needs no server. The DB file `cyber_alerts.db`
is created automatically in the working directory on first run
and seeded with sample users and alerts.

If you'd rather use Maven, just run `mvn clean package` from the
project root and skip this folder entirely.
