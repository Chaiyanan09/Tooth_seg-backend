# Tooth_seg-backend

Spring Boot 4 (Java 21) backend ของ ToothSeg ใช้ **PostgreSQL** เป็นฐานข้อมูล (เดิมใช้ MongoDB)

## เตรียมฐานข้อมูล (ครั้งแรกครั้งเดียว)

ติดตั้ง PostgreSQL (แนะนำเวอร์ชัน 18) แล้วเข้า `psql -U postgres -h localhost` จากนั้นรัน:

```sql
CREATE ROLE toothseg_app WITH LOGIN PASSWORD 'change-this-password';
CREATE DATABASE toothseg OWNER toothseg_app ENCODING 'UTF8';
```

ไม่ต้องสร้างตารางเอง ตอนแอปเริ่มทำงาน Flyway จะรันไฟล์ใน `src/main/resources/db/migration` ให้อัตโนมัติ

## ตั้งค่าการเชื่อมต่อ

ค่าเริ่มต้นอยู่ใน `application.properties` และ override ได้ด้วย environment variable:

| Environment variable | ค่าเริ่มต้น |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/toothseg` |
| `SPRING_DATASOURCE_USERNAME` | `toothseg_app` |
| `SPRING_DATASOURCE_PASSWORD` | (ว่าง ต้องตั้งเอง) |

ใน IntelliJ ตั้งได้ที่ Run → Edit Configurations → Environment variables เช่น
`SPRING_DATASOURCE_PASSWORD=change-this-password`

ถ้า hosting ให้ connection string แบบ `postgresql://user:pass@host:5432/db` ต้องแปลงเป็นรูปแบบ JDBC:
`SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db` แล้วใส่ user และ password แยกในอีกสองตัวแปร

## รันแอป

```powershell
./mvnw spring-boot:run
```

## รันเทสต์

เทสต์เชื่อมต่อ PostgreSQL จริง ต้องเปิดฐานข้อมูลและตั้ง `SPRING_DATASOURCE_PASSWORD` ก่อน:

```powershell
./mvnw test
```

ถ้าจะ build โดยไม่รันเทสต์: `./mvnw package -DskipTests`

## แก้โครงสร้างตาราง

ห้ามแก้ไฟล์ migration ที่รันไปแล้ว ให้สร้างไฟล์ใหม่เรียงเลขต่อไป เช่น
`src/main/resources/db/migration/V2__add_phone_to_users.sql` แล้วแก้ entity ให้ตรงกัน
ตอนเริ่มแอป Flyway จะรันเฉพาะไฟล์ที่ยังไม่เคยรัน และ Hibernate (`ddl-auto=validate`) จะตรวจว่า entity ตรงกับตาราง
