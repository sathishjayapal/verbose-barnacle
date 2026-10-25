import React, { useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useAuth } from 'app/common/auth-context';


export default function Header() {
  const { t } = useTranslation();
  const headerRef = useRef<HTMLElement|null>(null);
  const navigate = useNavigate();
  const { user, isAdmin, logout } = useAuth();

  const doLogout = async () => {
    await logout();
    navigate('/login');
  };

  const handleClick = (event: Event) => {
    // close any open dropdown
    const $clickedDropdown = (event.target as HTMLElement).closest('.js-dropdown');
    const $dropdowns = headerRef.current!.querySelectorAll('.js-dropdown');
    $dropdowns.forEach(($dropdown:Element) => {
      if ($clickedDropdown !== $dropdown && $dropdown.getAttribute('data-dropdown-keepopen') !== 'true') {
        $dropdown.ariaExpanded = 'false';
        $dropdown.nextElementSibling!.classList.add('hidden');
      }
    });
    // toggle selected if applicable
    if ($clickedDropdown) {
      $clickedDropdown.ariaExpanded = '' + ($clickedDropdown.ariaExpanded !== 'true');
      $clickedDropdown.nextElementSibling!.classList.toggle('hidden');
    }
  };

  useEffect(() => {
    document.body.addEventListener('click', handleClick);
    return () => document.body.removeEventListener('click', handleClick);
  }, []);

  return (
    <header ref={headerRef} className="bg-gray-50">
      <div className="container mx-auto px-4 md:px-6">
        <nav className="flex flex-wrap items-center justify-between py-2">
          <Link to="/" className="flex py-1.5 mr-4">
            <img src="/images/logo.png" alt={t('app.title')} width="30" height="30" className="inline-block" />
            <span className="text-xl pl-3">{t('app.title')}</span>
          </Link>
          <button type="button" className="js-dropdown md:hidden border rounded cursor-pointer" data-dropdown-keepopen="true"
              aria-label={t('navigation.toggle')} aria-controls="navbarToggle" aria-expanded="false">
            <div className="space-y-1.5 my-2.5 mx-4">
              <div className="w-6 h-0.5 bg-gray-500"></div>
              <div className="w-6 h-0.5 bg-gray-500"></div>
              <div className="w-6 h-0.5 bg-gray-500"></div>
            </div>
          </button>
          <div className="hidden md:block flex grow md:grow-0 justify-end basis-full md:basis-auto pt-3 md:pt-1 pb-1" id="navbarToggle">
            <ul className="flex items-center">
              {user && <>
                <li>
                  <Link to="/" className="block text-gray-500 p-2">{t('navigation.home')}</Link>
                </li>
                <li className="relative">
                  <button type="button" className="js-dropdown block text-gray-500 p-2 cursor-pointer" id="navbarEntitiesLink"
                      aria-expanded="false">
                    <span>{t('navigation.entities')}</span>
                    <span className="text-[9px] align-[3px] pl-0.5">&#9660;</span>
                  </button>
                  <ul className="hidden block absolute right-0 bg-white border border-gray-300 rounded min-w-[10rem] py-2" aria-labelledby="navbarEntitiesLink">
                    <li><Link to="/repositories" className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('repositories.list.headline')}</Link></li>
                    {isAdmin &&
                      <li><Link to="/repositories/add" className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('repositories.add.headline')}</Link></li>}
                  </ul>
                </li>
                <li className="px-2 py-2 text-xs text-gray-400 self-center">
                  {user.username}
                  <span className="ml-1 rounded bg-gray-200 px-1.5 py-0.5 text-gray-600">{isAdmin ? 'ADMIN' : 'VIEWER'}</span>
                </li>
                <li>
                  <button type="button" onClick={doLogout} className="block text-gray-500 p-2 cursor-pointer">{t('navigation.logout')}</button>
                </li>
              </>}
              {!user &&
                <li>
                  <Link to="/login" className="block text-gray-500 p-2">{t('navigation.login')}</Link>
                </li>}
            </ul>
          </div>
        </nav>
      </div>
    </header>
  );
}
