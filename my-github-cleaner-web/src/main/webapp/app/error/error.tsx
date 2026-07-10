import React from 'react';
import {Link, useLocation, useNavigate} from 'react-router';
import {useTranslation} from 'react-i18next';
import {getReasonPhrase} from 'http-status-codes';
import useDocumentTitle from 'app/common/use-document-title';


export default function Error() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  useDocumentTitle(t('error.page.headline'));

  const isErrorPath = location.pathname === '/error';
  const status = isErrorPath ? (location.state?.errorStatus || '503') : '404';
  const error = isErrorPath
      ? (location.state?.errorMessage || getReasonPhrase(status))
      : getReasonPhrase('404');

  const message = (() => {
    switch (status) {
      case '404':
        return t('error.page.notFound');
      case '500':
        return t('error.page.serverError');
      case '503':
        return t('error.page.serviceUnavailable');
      default:
        return t('error.page.message');
    }
  })();

  return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] text-center">
        <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-8 md:p-12 max-w-lg w-full">
          <div className="mb-6">
            <svg className="w-20 h-20 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>
          </div>
          <h1 className="text-6xl font-bold text-gray-800 mb-2">{status}</h1>
          <p className="text-xl text-gray-600 mb-4">{error}</p>
          <p className="text-gray-500 mb-8">{message}</p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/"
                  className="inline-flex items-center justify-center px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
              {t('error.page.backToRepositories')}
            </Link>
            {isErrorPath && (
                <button onClick={() => navigate(0)}
                        className="inline-flex items-center justify-center px-6 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors">
                  {t('error.page.tryAgain')}
                </button>
            )}
          </div>
        </div>
      </div>
  );
}
