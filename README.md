# MD04 - Session07: Config Server & Config Client (Spring Cloud Config)

## 1. Cấu trúc bài làm

| Thư mục | Vai trò |
|---|---|
| `medical-config-repo/` | Kho Git cục bộ (không push lên GitHub) chứa `patient-service.properties` - nguồn cấu hình tập trung. |
| `config-server/` | **Bài tập 1** - Spring Boot app dùng `spring-cloud-config-server`, đọc cấu hình từ `medical-config-repo`. |
| `patient-service/` | **Bài tập 2** - Spring Boot app dùng `spring-cloud-starter-config`, nạp toàn bộ cấu hình (bao gồm PostgreSQL) từ Config Server lúc khởi động thông qua `spring.config.import`. |

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
