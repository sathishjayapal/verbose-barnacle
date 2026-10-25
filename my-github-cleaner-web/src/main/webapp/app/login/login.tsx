import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router';
import useDocumentTitle from 'app/common/use-document-title';
import { useAuth } from 'app/common/auth-context';


export default function Login() {
  const { t } = useTranslation();
  useDocumentTitle(t('login.headline'));

  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const doLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      await login(username, password);
      // send the user back to the page they originally requested
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== '/login' ? from : '/repositories');
    } catch {
      setError(t('login.failed'));
    }
  };

  return (
    <div className="max-w-sm mx-auto mt-16">
      <h1 className="text-3xl md:text-4xl font-medium mb-6">{t('login.headline')}</h1>
      {error && <p className="bg-red-200 border-red-800 text-red-800 border rounded p-4 mb-6" role="alert">{error}</p>}
      <form onSubmit={doLogin} noValidate>
        <div className="mb-4">
          <label htmlFor="username" className="block text-gray-600 mb-1">{t('login.username')}</label>
          <input id="username" type="text" value={username} required autoFocus
              onChange={(e) => setUsername(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2" />
        </div>
        <div className="mb-4">
          <label htmlFor="password" className="block text-gray-600 mb-1">{t('login.password')}</label>
          <input id="password" type="password" value={password} required
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2" />
        </div>
        <input type="submit" value={t('login.submit')}
            className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300 focus:ring-4 rounded px-5 py-2 cursor-pointer" />
      </form>
    </div>
  );
}
