import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useSearchParams } from 'react-router';


export function Pagination({ page }: PaginationParams) {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const [searchParams, ] = useSearchParams();
  const steps: PaginationStep[] = [];
  let range = '';

  const getStepParams = (targetPage: number) => {
    const stepParams = new URLSearchParams({
      page: '' + targetPage,
      size: '' + page!.size
    });
    if (searchParams.get('sort')) {
      stepParams.append('sort', searchParams.get('sort')!);
    }
    if (searchParams.get('filter')) {
      stepParams.append('filter', searchParams.get('filter')!);
    }
    return stepParams;
  };

  if (page) {
    const previous = new PaginationStep();
    previous.disabled = page.number === 0;
    previous.label = t('pagination.previous');
    previous.params = getStepParams(Math.max(0, page.number - 1));
    steps.push(previous);
    // find a range of up to 5 pages around the current active page
    const startAt = Math.max(0, Math.min(page.number - 2, page.totalPages - 5));
    const endAt = Math.min(startAt + 5, page.totalPages);
    for (let i = startAt; i < endAt; i++) {
      const step = new PaginationStep();
      step.active = i === page.number;
      step.label = '' + (i + 1);
      step.params = getStepParams(i);
      steps.push(step);
    }
    const next = new PaginationStep();
    next.disabled = page.number > page.totalPages - 2;
    next.label = t('pagination.next');
    next.params = getStepParams(Math.min(page.totalPages - 1, page.number + 1));
    steps.push(next);

    const rangeStart = page.number * page.size + 1;
    const rangeEnd = Math.min(rangeStart + page.size - 1, page.totalElements);
    if (rangeStart === rangeEnd) {
      range = '' + rangeStart;
    } else {
      range = rangeStart + '-' + rangeEnd;
    }
  }

  return (
    <div className="grid grid-cols-12 mt-10">
      {steps.length > 3 &&
      <nav className="col-span-8">
        <ul className="flex">
          {steps.map((step) => (
          <li className={'group' + (step.active ? ' ' : '') + (step.disabled ? ' ' : '')} key={step.label}>
            <Link to={{ pathname: pathname, search: step.params.toString() }} className={'border-y border-l group-first:rounded-l group-last:rounded-r group-last:border-r border-gray-300 py-2 px-3.5' + (step.active ? ' bg-blue-600 text-white border-blue-600' : '') + (step.disabled ? ' pointer-events-none text-gray-400' : ' hover:bg-gray-100')}>{step.label}</Link>
          </li>
          ))}
        </ul>
      </nav>
      }
      <div className={'col-span-4 sm:text-right mt-4 sm:mt-0' + (steps.length <= 3 ? ' col-start-9' : '')}>{t('pagination.elements', { range: range, total: page!.totalElements })}</div>
    </div>
  );
}

export interface Page {

  totalElements: number;
  totalPages: number;
  number: number;
  size: number;

}

export interface PagedModel<PageType> {

  content: PageType[];
  page: Page;

}

class PaginationStep {

  active = false;
  disabled = false;
  label = '';
  params: URLSearchParams = new URLSearchParams();

}

interface PaginationParams {

  page?: Page;

}
