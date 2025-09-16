# Security Configuration Guide

This document explains how to configure authentication credentials for the GitHub Cleaner application.

## Frontend Configuration

The frontend uses environment variables for API authentication. These can be configured in two ways:

### Option 1: Environment Variables (Recommended for Production)
Set the following environment variables:
```bash
export REACT_APP_AUTH_USERNAME=your_username
export REACT_APP_AUTH_PASSWORD=your_password
```

### Option 2: .env File (Development)
Create a `.env` file in the webapp root directory:
```
REACT_APP_AUTH_USERNAME=viewer
REACT_APP_AUTH_PASSWORD=Bootify!
```


## Password Encoding

The current bcrypt hash corresponds to the password "Bootify!". To generate new bcrypt hashes:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("your_new_password");
log.error("{bcrypt}" + hashedPassword);
```

## Security Best Practices

1. **Never commit credentials to version control**
2. **Use environment variables in production**
3. **Rotate passwords regularly**
4. **Use strong, unique passwords**
5. **Consider using external authentication providers for production**
