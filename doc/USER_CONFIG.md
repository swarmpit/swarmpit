# Swarmpit User Configuration

Swarmpit now supports configuring users via a YAML file. This allows you to automatically create users when the service starts, with passwords specified in several ways.

## Configuration File

Create a `users.yaml` file with your user configurations. Here's an example:

```yaml
users:
  - username: admin
    email: admin@example.com
    role: admin
    password_hash: "$2a$10$RzUwGn1D4czuPqG62XGBa.Ltz4rsKxG7rXwVcdDIkIVJ7WqZlscwy"

  - username: viewer
    email: viewer@example.com
    role: viewer
    password_env: VIEWER_PASSWORD

  - username: user
    email: user@example.com
    role: user
    password_secret: user_password_secret
```

## Password Configuration Methods

You can specify passwords in three ways:

1. **Direct Password** (not recommended for production):
   ```yaml
   password: mypassword
   ```

2. **Environment Variable**:
   ```yaml
   password_env: USER_PASSWORD
   ```

3. **Docker Secret**:
   ```yaml
   password_secret: user_password_secret
   ```

## Usage with Docker Compose

1. Create your `users.yaml` file
2. Mount it in your docker-compose.yml:
   ```yaml
   services:
     app:
       volumes:
         - ./users.yaml:/run/configs/users.yaml:ro
   ```

3. If using secrets, declare them:
   ```yaml
   secrets:
     user_password_secret:
       external: true
   ```

4. If using environment variables, declare them:
   ```yaml
   environment:
     - USER_PASSWORD=mypassword
   ```

## Notes

- Users are only created if they don't already exist in the database
- Passwords must be at least 8 characters long
- Usernames must be at least 4 characters long
- Available roles are: admin, user, viewer
- If no role is specified, "viewer" is used as default
- The configuration file is optional - if not mounted, Swarmpit will work as before
- Password priority: password_hash > password > password_env > password_secret
- For production environments, using password_hash or password_secret is recommended 