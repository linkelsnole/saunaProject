export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthUser {
  id: string;
  username: string;
  role: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: AuthUser;
}
