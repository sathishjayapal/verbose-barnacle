import React from 'react';
import { act, render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import App from './app';


test('should create the app', async () => {
  const router = createMemoryRouter([
    { path: '', element: <App /> }
  ]);
  const { container } = await act(async () => render(<RouterProvider router={router} />));
  expect(container).toBeTruthy();
});
