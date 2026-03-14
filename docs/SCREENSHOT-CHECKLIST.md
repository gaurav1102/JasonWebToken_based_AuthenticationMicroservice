# Screenshot Checklist

Run the service for screenshots with:

```bash
./gradlew app:bootRun --args='--server.port=9900 --spring.profiles.active=demo'
```

- Swagger UI home page
- Postman collection overview
- Signup success response
- Login success response
- Refresh-token success response
- Logout success response
- Protected `/api/v1/me` success response
- Protected `/api/v1/admin` unauthorized response for non-admin user
- Database screenshot showing `users`, `roles`, and `refresh_tokens`
- GitHub Actions CI passing
