FROM eclipse-temurin:21-jre-ubi9-minimal
LABEL authors="eflerrr"

WORKDIR /app

COPY jar/device-fat.jar .

CMD ["java", "-jar", "device-fat.jar"]
