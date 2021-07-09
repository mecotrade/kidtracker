#!/bin/bash

rm -f /build/src/main/resources/kidtracker.p12
rm -rf /build/node_modules
keytool -genkeypair -keystore /build/src/main/resources/kidtracker.p12 -storetype PKCS12 -storepass 12345678 -alias kidtracker -keyalg RSA -keysize 2048 -validity 99999 -dname "CN=Kidtracker SSL Certificate, OU=Kidtracker, O=Kidtracker, C=SA" -ext san=dns:${DOMAIN:-localhost},ip:${IP:-127.0.0.1}
npm install
mvn clean package -P with-frontend -Dmaven.test.skip=true