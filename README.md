# MD04 - Session07: Thiết lập Config Server & Lưu trữ cấu hình trên Git

## 1. Cấu trúc bài làm

Bài thực hành gồm 2 phần:

1. **Local Git config repo** (`medical-config-repo/`) - thư mục chạy `git init` cục bộ,
   chứa file `patient-service.properties` với dữ liệu cấu hình mẫu cho `patient-service`.
   Thư mục này chỉ tồn tại trên máy cục bộ (không push lên GitHub) vì đây là "kho lưu trữ"
   mà Config Server trỏ tới thông qua đường dẫn `file://...`, tương tự một Git repository
   cấu hình riêng trong thực tế.
2. **`config-server/`** - project Spring Boot (Maven) chứa dependency
   `spring-cloud-config-server`, đây là mã nguồn được push lên GitHub.

## 2. Nội dung `patient-service.properties` (mẫu)

```properties
server.port=8081
spring.application.name=patient-service

patient.service.message=Xin chao tu Patient Service - Config Server
patient.service.version=1.0.0
patient.service.max-appointment-per-day=50
```

## 3. Cấu hình Config Server

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

> Nhánh mặc định của repo cục bộ là `main` nên `default-label` được đặt là `main`
> (nếu nhánh của bạn là `master` thì đổi giá trị này thành `master`).

## 4. Cách chạy & kiểm tra

```bash
cd config-server
./mvnw spring-boot:run
```

Truy cập: http://localhost:8888/patient-service/default

Kết quả trả về (đã kiểm thử thành công):

```json
{
  "name": "patient-service",
  "profiles": ["default"],
  "label": null,
  "version": "d057a7caf1ccef68232da740c00dd64e392915d0",
  "state": "",
  "propertySources": [
    {
      "name": "file:///.../medical-config-repo/patient-service.properties",
      "source": {
        "server.port": "8081",
        "spring.application.name": "patient-service",
        "patient.service.message": "Xin chao tu Patient Service - Config Server",
        "patient.service.version": "1.0.0",
        "patient.service.max-appointment-per-day": "50"
      }
    }
  ]
}
```
