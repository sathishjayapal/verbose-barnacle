import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { RepositoriesDTO } from 'app/repositories/repositories-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


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

export default function RepositoriesEdit() {
  const { t } = useTranslation();
  useDocumentTitle(t('repositories.edit.headline'));

  const navigate = useNavigate();
  const params = useParams();
  const currentId = +params.id!;

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const getMessage = (key: string) => {
    const messages: Record<string, string> = {
      REPOSITORIES_REPO_NAME_UNIQUE: t('exists.repositories.repoName')
    };
    return messages[key];
  };

  const prepareForm = async () => {
    try {
      const data = (await axios.get('/api/repositoriess/' + currentId)).data;
      useFormResult.reset(data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareForm();
  }, []);

  const updateRepositories = async (data: RepositoriesDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.put('/api/repositoriess/' + currentId, data);
      navigate('/repositoriess', {
            state: {
              msgSuccess: t('repositories.update.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t, getMessage);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('repositories.edit.headline')}</h1>
      <div>
        <Link to="/repositoriess" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('repositories.edit.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(updateRepositories)} noValidate>
      <InputRow useFormResult={useFormResult} object="repositories" field="id" disabled={true} type="number" />
      <InputRow useFormResult={useFormResult} object="repositories" field="repoName" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="repositories" field="repoCreatedDate" required={true} type="datetimepicker" />
      <InputRow useFormResult={useFormResult} object="repositories" field="repoUpdatedDate" required={true} type="datetimepicker" />
      <InputRow useFormResult={useFormResult} object="repositories" field="cloneUrl" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="repositories" field="description" required={true} type="textarea" />
      <input type="submit" value={t('repositories.edit.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
