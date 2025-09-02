import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { handleServerError, getListParams } from 'app/common/utils';
import { RepositoriesDTO } from 'app/repositories/repositories-model';
import { PagedModel, Pagination } from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';
import { getAuthConfig, getBasicAuthHeader } from 'app/config/auth-config';



export default function RepositoriesList() {
  const { t } = useTranslation();
  useDocumentTitle(t('repositories.list.headline'));

  const [repositorieses, setRepositorieses] = useState<PagedModel<RepositoriesDTO>|undefined>(undefined);
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'id,ASC': t('repositories.list.sort.id,ASC'), 
    'repoName,ASC': t('repositories.list.sort.repoName,ASC'), 
    'repoCreatedDate,ASC': t('repositories.list.sort.repoCreatedDate,ASC')
  };

  const getAllRepositorieses = async () => {
    try {
      const response = await axios.get('/api/repositories?' + listParams,
          { headers:
            { authorization: getBasicAuthHeader() } });
      setRepositorieses(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (id: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      const authConfig = getAuthConfig();
      axios.defaults.auth = {
        username: authConfig.username,
        password: authConfig.password
      };
      await axios.delete('/api/repositories/' + id);
      navigate('/repositories', {
            state: {
              msgInfo: t('repositories.delete.success')
            }
          });
      getAllRepositorieses();
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllRepositorieses();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('repositories.list.headline')}</h1>
      <div>
        <Link to="/repositories/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('repositories.list.createNew')}</Link>
      </div>
    </div>
    {((repositorieses && repositorieses.page.totalElements !== 0) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('repositories.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!repositorieses || repositorieses.page.totalElements === 0 ? (
    <div>{t('repositories.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('repositories.id.label')}</th>
            <th scope="col" className="text-left p-2">{t('repositories.repoName.label')}</th>
            <th scope="col" className="text-left p-2">{t('repositories.description.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {repositorieses.content.map((repositories) => (
          <tr key={repositories.id} className="odd:bg-gray-100">
            <td className="p-2">{repositories.id}</td>
            <td className="p-2">{repositories.repoName}</td>
            <td className="p-2">{repositories.description}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/repositories/edit/' + repositories.id} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('repositories.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(repositories.id!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('repositories.list.delete')}</button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    <Pagination page={repositorieses.page} />
    </>)}
  </>);
}
