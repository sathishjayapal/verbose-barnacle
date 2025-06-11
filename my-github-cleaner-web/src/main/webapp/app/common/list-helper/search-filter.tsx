import React, { FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useLocation, useSearchParams } from 'react-router';


export default function SearchFilter({ placeholder }: SearchFilterParams) {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();

  const handleSubmit = (event: FormEvent) => {
    const input = (event.target as HTMLFormElement)!.querySelector('input') as HTMLInputElement;
    const filterParams = new URLSearchParams({
      filter: input.value
    });
    navigate({
      pathname: pathname,
      search: filterParams.toString()
    });
    event.preventDefault();
  };

  return (
    <form onSubmit={handleSubmit} method="get" className="w-full md:w-1/2 xl:w-1/3 md:pr-2">
      <div className="mb-3 w-full flex flex-wrap items-stretch">
        <input type="text" name="filter" defaultValue={searchParams.get('filter') || ''} placeholder={placeholder} className="border-gray-300 rounded-l flex-[1_1_auto]" />
        <button type="submit" className="text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded-r px-5 py-2">{t('searchFilter.apply')}</button>
      </div>
    </form>
  );
}

interface SearchFilterParams {

  placeholder: string;

}
