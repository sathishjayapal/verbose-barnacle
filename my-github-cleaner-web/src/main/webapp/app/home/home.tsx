import React from 'react';
import { Link } from 'react-router';
import { Trans, useTranslation } from 'react-i18next';
import useDocumentTitle from 'app/common/use-document-title';
import './home.scss';


export default function Home() {
  const { t } = useTranslation();
  useDocumentTitle(t('home.index.headline'));

  return (<>
    <h1 className="grow text-3xl md:text-4xl font-medium mb-8">{t('home.index.headline')}</h1>
    <p className="mb-12"><Trans i18nKey="home.index.text" components={{ a: <a />, strong: <strong /> }} /></p>
    <div className="md:w-2/5 mb-12">
      <h4 className="text-2xl font-medium mb-4">{t('home.index.exploreEntities')}</h4>
      <div className="flex flex-col border border-gray-300 rounded">
        <Link to="/repositoriess" className="w-full border-gray-300 hover:bg-gray-100 rounded-t rounded-b px-4 py-2">{t('repositories.list.headline')}</Link>
      </div>
    </div>
  </>);
}
