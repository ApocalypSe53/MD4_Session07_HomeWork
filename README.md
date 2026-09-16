# MD04 - Session07: Config Server & Config Client (Spring Cloud Config)

## 1. Cấu trúc bài làm

| Thư mục | Vai trò |
|---|---|
| `medical-config-repo/` | Kho Git cục bộ (không push lên GitHub) chứa `patient-service.properties` - nguồn cấu hình tập trung. |
| `config-server/` | **Bài tập 1** - Spring Boot app dùng `spring-cloud-config-server`, đọc cấu hình từ `medical-config-repo`. |
| `patient-service/` | **Bài tập 2 & 3** - Spring Boot app dùng `spring-cloud-starter-config`, nạp toàn bộ cấu hình (DB, port...) từ Config Server lúc khởi động thông qua `spring.config.import`. Bài 3 bổ sung 2 profile `dev` (MySQL) và `prod` (PostgreSQL). |

`medical-config-repo/` chỉ tồn tại cục bộ vì nó đóng vai trò "kho cấu hình từ xa" mà Config Server trỏ tới qua `file://...` - trong thực tế đây thường là một Git repository riêng biệt, không nằm trong mã nguồn ứng dụng.

## 2. Bài tập 1 - Config Server

`config-server/src/main/java/.../ConfigServerApplication.java`:

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication { ... }
```

`config-server/src/main/resources/application.properties`:

```properties
spring.application.name=config-server
server.port=8888

spring.cloud.config.server.git.uri=file:///C:/Users/Vudin/OneDrive/Documents/Rikkei/MD04/Session07/medical-config-repo
spring.cloud.config.server.git.default-label=main
```

> Nhánh mặc định của repo cục bộ là `main` nên `default-label` đặt là `main`
> (nếu nhánh của bạn là `master` thì đổi giá trị này thành `master`).

Chạy và kiểm tra:

```bash
cd config-server
./mvnw spring-boot:run
```

Truy cập http://localhost:8888/patient-service/default để xem cấu hình dạng JSON.

## 3. Bài tập 2 - Patient Service kết nối Config Server

**`patient-service/pom.xml`**: thêm dependency `spring-cloud-starter-config`.

**`patient-service/src/main/resources/application.properties`** (local, KHÔNG chứa thông tin DB):

```properties
spring.application.name=patient-service
spring.config.import=configserver:http://localhost:8888
```

**`medical-config-repo/patient-service.properties`** (trên Git - chứa toàn bộ cấu hình PostgreSQL):

```properties
server.port=8081
spring.application.name=patient-service

patient.service.message=Xin chao tu Patient Service - Config Server
patient.service.version=1.0.0
patient.service.max-appointment-per-day=50

# PostgreSQL - patient-service (moved here from application.properties)
spring.datasource.url=jdbc:postgresql://localhost:5433/patient_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

> **Lưu ý cổng 5433**: máy dùng để kiểm thử bài này đã có sẵn một PostgreSQL cài native
> chiếm cổng 5432, nên khi kiểm thử, một container PostgreSQL Docker riêng được dùng ở
> cổng **5433** (`docker run -e POSTGRES_DB=patient_db -e POSTGRES_USER=postgres
> -e POSTGRES_PASSWORD=postgres -p 5433:5432 -d postgres:16`) để không đụng vào
> database thật trên máy. Nếu bạn chạy trên máy khác chỉ có một PostgreSQL ở cổng
> mặc định, hãy đổi lại thành `5432` và đúng username/password của bạn.

Chạy theo thứ tự: PostgreSQL → `config-server` → `patient-service`:

```bash
cd config-server && ./mvnw spring-boot:run   # cổng 8888
cd patient-service && ./mvnw spring-boot:run # cổng 8081
```

Log khởi động thành công (đã kiểm thử thực tế), không có `spring.datasource.*` nào
trong `application.properties` cục bộ:

```
ConfigServerConfigDataLoader : Fetching config from server at : http://localhost:8888
ConfigServerConfigDataLoader : Located environment: name=patient-service, profiles=[default], ...
HikariDataSource             : HikariPool-1 - Start completed.
PatientServiceApplication    : Started PatientServiceApplication in 4.379 seconds
```

### Ghi chú xử lý sự cố (timezone)

Trên máy Windows có timezone hệ thống là `Asia/Saigon` (tên gọi cũ/alias của
`Asia/Ho_Chi_Minh`), driver PostgreSQL JDBC tự động gửi tên timezone này lên server khi
mở kết nối. Một số bản build PostgreSQL (vd. image Docker `postgres:16`) không có alias
`Asia/Saigon` trong bảng timezone của nó và sẽ từ chối kết nối với lỗi:

```
FATAL: invalid value for parameter "TimeZone": "Asia/Saigon"
```

Cách khắc phục (đã áp dụng trong `patient-service/.../PatientServiceApplication.java`):
đặt timezone mặc định của JVM về tên chuẩn IANA `Asia/Ho_Chi_Minh` trước khi Spring
Boot khởi động:

```java
public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    SpringApplication.run(PatientServiceApplication.class, args);
}
```

## 4. Bài tập 3 - Quản lý cấu hình đa môi trường (Profiles)

**`patient-service/pom.xml`**: thêm dependency `mysql-connector-j` (bên cạnh `postgresql`
đã có) để cùng một mã nguồn có thể chạy với cả 2 hệ CSDL tuỳ theo profile.

**`patient-service/src/main/resources/application.properties`** (local - không đổi so
với bài 2, chỉ giữ cấu hình trỏ tới Config Server):

```properties
spring.application.name=patient-service
spring.config.import=configserver:http://localhost:8888
```

**`medical-config-repo/patient-service-dev.properties`** (Dev - MySQL, cổng 8081):

```properties
server.port=8081

spring.datasource.url=jdbc:mysql://localhost:3308/patient_db_dev
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

**`medical-config-repo/patient-service-prod.properties`** (Prod - PostgreSQL, cổng 8082):

```properties
server.port=8082

spring.datasource.url=jdbc:postgresql://localhost:5433/patient_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

> **Lưu ý cổng DB dùng để kiểm thử**: máy kiểm thử đã có sẵn MySQL native (cổng 3306)
> và PostgreSQL native (cổng 5432), nên khi test dùng 2 container Docker riêng ở cổng
> **3308** (MySQL) và **5433** (PostgreSQL) để không đụng vào database thật trên máy.
> Đổi lại thành cổng/username/password thật của bạn nếu cần.

Sau khi tạo 2 file, nhớ `git add .` + `git commit` trong `medical-config-repo` để Config
Server nhận diện (Config Server tự `git pull` mỗi lần có request nên không cần khởi động
lại).

Kiểm tra Config Server đã nhận diện đúng profile:

- http://localhost:8888/patient-service/dev → property source đầu tiên là
  `patient-service-dev.properties` (ghi đè `server.port=8081` và toàn bộ `spring.datasource.*`
  sang MySQL), sau đó vẫn liệt kê `patient-service.properties` (base) với độ ưu tiên thấp hơn.
- http://localhost:8888/patient-service/prod → tương tự nhưng với `patient-service-prod.properties`
  (`server.port=8082`, PostgreSQL) ở độ ưu tiên cao nhất.

Chạy patient-service với từng profile (tương đương "Active profiles" trong IntelliJ Run
Configuration):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # port 8081, MySQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod  # port 8082, PostgreSQL
```

Log đã kiểm thử thực tế:

```
# --spring.profiles.active=dev
PatientServiceApplication : The following 1 profile is active: "dev"
TomcatWebServer           : Tomcat initialized with port 8081 (http)
HikariPool                : Added connection com.mysql.cj.jdbc.ConnectionImpl@...
HikariDataSource           : HikariPool-1 - Start completed.
PatientServiceApplication : Started PatientServiceApplication in 8.38 seconds

# --spring.profiles.active=prod
PatientServiceApplication : The following 1 profile is active: "prod"
TomcatWebServer           : Tomcat initialized with port 8082 (http)
HikariPool                : Added connection org.postgresql.jdbc.PgConnection@...
HikariDataSource           : HikariPool-1 - Start completed.
PatientServiceApplication : Started PatientServiceApplication in 6.392 seconds
```

## 5. Bài tập 4 - Tự động cập nhật cấu hình với `@RefreshScope`

**`patient-service/pom.xml`**: thêm dependency `spring-boot-starter-actuator`.

**`patient-service/src/main/resources/application.properties`** (local): expose endpoint
`refresh` của Actuator (mặc định Spring Boot chỉ public `health`):

```properties
spring.application.name=patient-service
spring.config.import=configserver:http://localhost:8888

management.endpoints.web.exposure.include=refresh
```

**`medical-config-repo/patient-service.properties`**: thêm

```properties
app.welcome=Chao mung toi BV RikkeiAcademy
```

**`patient-service/src/main/java/.../WelcomeController.java`**:

```java
@RestController
@RefreshScope
public class WelcomeController {

    @Value("${app.welcome}")
    private String welcomeMessage;

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }
}
```

Quy trình đã kiểm thử thực tế, đúng từng bước:

1. Chạy `config-server` rồi `patient-service` (mặc định, cổng 8081).
2. `GET http://localhost:8081/welcome` → `Chao mung toi BV RikkeiAcademy`.
3. Sửa `app.welcome=Chao mung toi BV RikkeiEducation` trong `medical-config-repo/patient-service.properties`,
   `git add` + `git commit`.
4. `GET /welcome` lúc này **vẫn trả về giá trị cũ** (đúng như kỳ vọng - bean `@RefreshScope`
   chỉ tạo lại khi có sự kiện refresh, không tự động poll Git liên tục).
5. `POST http://localhost:8081/actuator/refresh` → trả về danh sách property đã đổi:
   `["config.client.version","app.welcome"]`.
6. `GET /welcome` → `Chao mung toi BV RikkeiEducation` - **đã đổi ngay lập tức**.
7. Xác nhận ứng dụng **không restart**: cùng PID và cùng `CreationDate` của tiến trình
   Java trước/sau bước refresh (`ProcessId 25548`, `CreationDate 9/16/2026 12:13:19 PM`
   ở cả hai lần kiểm tra).
