import React from 'react';
import {createBrowserRouter, RouterProvider} from 'react-router';
import App from "./app";
import RepositoriesList from './repositories/repositories-list';
import RepositoriesAdd from './repositories/repositories-add';
import RepositoriesEdit from './repositories/repositories-edit';
import Error from './error/error';
import Login from './login/login';
import {AuthProvider} from './common/auth-context';
import {RedirectIfAuthed, RequireAuth} from './common/auth-guards';


export default function AppRoutes() {
  const router = createBrowserRouter([
    {
      element: <App />,
      children: [
        // Public — already-authenticated users get bounced to /repositories
        { path: 'login', element: <RedirectIfAuthed><Login /></RedirectIfAuthed> },
        { path: 'error', element: <Error /> },

        // Any authenticated role (VIEWER or ADMIN)
        {
          element: <RequireAuth />,
          children: [
            { path: '', element: <RepositoriesList /> },
            { path: 'repositories', element: <RepositoriesList /> },
          ]
        },

        // ADMIN-only — mirrors @PreAuthorize("hasAuthority('ADMIN')") on the API
        {
          element: <RequireAuth roles={['ADMIN']} />,
          children: [
            { path: 'repositories/add', element: <RepositoriesAdd /> },
            { path: 'repositories/edit/:id', element: <RepositoriesEdit /> },
          ]
        },

        { path: '*', element: <Error /> }
      ]
    }
  ]);

  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
