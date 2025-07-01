FROM maven:3.8.3-openjdk-17 AS builder

WORKDIR /app
COPY pom.xml .
# Tải dependencies trước để tận dụng cache Docker layer
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime với JRE nhẹ
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Tối ưu cho container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080

# Security best practice - chạy bằng user không có quyền root
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]