// Authentication configuration
import { DEFAULT_CONFIG } from './default-config';

export interface AuthConfig {
  username: string;
  password: string;
}

export const getAuthConfig = (): AuthConfig => {
  return {
    username: process.env.REACT_APP_AUTH_USERNAME || DEFAULT_CONFIG.AUTH.USERNAME,
    password: process.env.REACT_APP_AUTH_PASSWORD || DEFAULT_CONFIG.AUTH.PASSWORD
  };
};

export const getBasicAuthHeader = (): string => {
  const config = getAuthConfig();
  console.error('Using basic authentication with username:', config.password);
  return 'Basic ' + window.btoa(config.username + ':' + config.password);
};
