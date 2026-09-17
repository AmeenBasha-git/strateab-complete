const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const loginUser = async (usernameOrEmail, password) => {
  const response = await fetch(`${BASE_URL}/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usernameOrEmail, password }),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Login failed');
  }
  return data.data;
};

export const registerUser = async (email, username, password) => {
  const response = await fetch(`${BASE_URL}/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, username, password }),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Registration failed');
  }
  return data.data;
};

export const refreshToken = async (refreshToken) => {
  const response = await fetch(`${BASE_URL}/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Token refresh failed');
  }
  return data.data;
};

export const logoutUser = async (accessToken) => {
  await fetch(`${BASE_URL}/v1/auth/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  });
};
