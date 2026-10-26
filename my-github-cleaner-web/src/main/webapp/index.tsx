import React from 'react';
import ReactDOM from 'react-dom/client';
import { initReactI18next } from 'react-i18next';
import i18n from 'i18next';
import axios from 'axios';
import translation from './translation.json';
import AppRoutes from './app/routes';
import './index.css';


i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: translation },
    },
    lng: 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    }
  });

axios.defaults.baseURL = process.env.API_PATH || '';
axios.defaults.withCredentials = true;
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

const root = document.getElementById('root')!!;
ReactDOM.createRoot(root).render(
  <AppRoutes />
);
