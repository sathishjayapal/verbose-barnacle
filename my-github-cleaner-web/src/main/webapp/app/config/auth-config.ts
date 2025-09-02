// Authentication configuration
export interface AuthConfig {
  username: string;
  password: string;
}

// Default configuration - should be overridden by environment variables
const defaultConfig: AuthConfig = {
  username: process.env.REACT_APP_AUTH_USERNAME || '',
  password: process.env.REACT_APP_AUTH_PASSWORD || ''
};

export const getAuthConfig = (): AuthConfig => {
  return {
    username: process.env.REACT_APP_AUTH_USERNAME || defaultConfig.username,
    password: process.env.REACT_APP_AUTH_PASSWORD || defaultConfig.password
  };
};

export const getBasicAuthHeader = (): string => {
  const config = getAuthConfig();
  console.error('Using basic authentication with username:', config.password);
  return 'Basic ' + window.btoa(config.username + ':' + config.password);
};
