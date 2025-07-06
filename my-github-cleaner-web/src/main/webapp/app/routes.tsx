import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import App from "./app";
import Home from './home/home';
import RepositoriesList from './repositories/repositories-list';
import RepositoriesAdd from './repositories/repositories-add';
import RepositoriesEdit from './repositories/repositories-edit';
import Error from './error/error';


export default function AppRoutes() {
  const router = createBrowserRouter([
    {
      element: <App />,
      children: [
        { path: '', element: <Home /> },
        { path: 'repositories', element: <RepositoriesList /> },
        { path: 'repositories/add', element: <RepositoriesAdd /> },
        { path: 'repositories/edit/:id', element: <RepositoriesEdit /> },
        { path: 'error', element: <Error /> },
        { path: '*', element: <Error /> }
      ]
    }
  ]);

  return (
    <RouterProvider router={router} />
  );
}
