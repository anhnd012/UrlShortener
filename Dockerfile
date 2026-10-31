# 1. Chọn base image có Java Runtime
FROM eclipse-temurin:21-jre

# 2. Thiết lập working directory
WORKDIR /app

# 3. Copy file JAR vào image
COPY target/UrlShorten-0.0.1-SNAPSHOT.jar app.jar

# 4. Khai báo cổng ứng dụng
EXPOSE 8080

# 5. Lệnh chạy khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]