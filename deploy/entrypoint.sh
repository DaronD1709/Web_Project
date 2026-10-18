#!/bin/sh
# Nhieu nen tang cloud cap cong qua bien PORT: doi cong cua Tomcat theo PORT (mac dinh 8080).
set -e
PORT="${PORT:-8080}"
sed -i "s/port=\"8080\"/port=\"${PORT}\"/" /usr/local/tomcat/conf/server.xml
exec catalina.sh run
