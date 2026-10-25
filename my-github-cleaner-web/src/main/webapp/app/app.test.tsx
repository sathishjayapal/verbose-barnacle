import React from 'react';
import { act, render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import axios from 'axios';
import App from './app';
import { AuthProvider } from './common/auth-context';

jest.mock('axios');

test('should create the app', async () => {
  // no session in tests — /api/auth/me rejects and user stays logged out
  (axios.get as jest.Mock).mockRejectedValue(new Error('unauthorized'));

  const router = createMemoryRouter([
    { path: '', element: <App /> }
  ]);
  const { container } = await act(async () => render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  ));
  expect(container).toBeTruthy();
});
