import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { RepositoriesDTO } from 'app/repositories/repositories-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';
import { getAuthConfig } from 'app/config/auth-config';


function getSchema() {
  setYupDefaults();
  return yup.object({
    repoName: yup.string().emptyToNull().required(),
    repoCreatedDate: yup.string().emptyToNull().required(),
    repoUpdatedDate: yup.string().emptyToNull().required(),
    cloneUrl: yup.string().emptyToNull().required(),
    description: yup.string().emptyToNull().required()
  });
}

export default function RepositoriesAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('repositories.add.headline'));

  const navigate = useNavigate();

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const getMessage = (key: string) => {
    const messages: Record<string, string> = {
      REPOSITORIES_REPO_NAME_UNIQUE: t('exists.repositories.repoName')
    };
    return messages[key];
  };

  const createRepositories = async (data: RepositoriesDTO) => {
    window.scrollTo(0, 0);
    try {
      console.error(data);
      const authConfig = getAuthConfig();
      await axios.post('/api/repositories', data, {
        auth: {
          username: authConfig.username,
          password: authConfig.password
        }
      });
      navigate('/repositories', {
            state: {
              msgSuccess: t('repositories.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t, getMessage);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('repositories.add.headline')}</h1>
      <div>
        <Link to="/repositories" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('repositories.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createRepositories)} noValidate>
      <InputRow useFormResult={useFormResult} object="repositories" field="repoName" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="repositories" field="repoCreatedDate" required={true} type="datetimepicker" />
      <InputRow useFormResult={useFormResult} object="repositories" field="repoUpdatedDate" required={true} type="datetimepicker" />
      <InputRow useFormResult={useFormResult} object="repositories" field="cloneUrl" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="repositories" field="description" required={true} type="textarea" />
      <input type="submit" value={t('repositories.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 mt-6" />
    </form>
  </>);
}
