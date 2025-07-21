import React, { ChangeEvent } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router';


export default function Sorting({ sortOptions, rowClass }: SortingParams) {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();

  const handleChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const sortParams = new URLSearchParams({
      sort: event.target!.value
    });
    if (searchParams.get('filter')) {

      sortParams.append('filter', searchParams.get('filter')!);
    }
    navigate({
      pathname: pathname,
      search: sortParams.toString()
    });
  };

  return (
    <div className={'w-full md:w-1/2 xl:w-1/3 ' + (rowClass || '')}>
      <select onChange={handleChange} value={searchParams.get('sort') || Object.keys(sortOptions)[0]} className="border-gray-300 rounded w-full">
        {Object.entries(sortOptions).map(([key, value]) => (
          <option value={key} key={key}>{value}</option>
        ))}
      </select>
    </div>
  );
}

interface SortingParams {

  sortOptions: Record<string, string>;
  rowClass?: string;

}
