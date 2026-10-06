// src/app/services/two-factor-auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface TwoFactorSetupResponse {
  secret: string;
  qrCodeUrl: string;
  backupCodes: string[];
}

export interface TwoFactorMassEnableResponse {
  activatedCount: number;
  userSecrets: Record<string, string>;
  userBackupCodes: Record<string, string[]>;
}

export interface TwoFactorStatusResponse {
  twoFactorEnabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TwoFactorAuthService {
  private apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  generateTwoFactorSecret(userId: string): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>(`${this.apiUrl}/2fa/generate/${userId}`, {});
  }

  verifyAndEnableTwoFactor(userId: string, otpCode: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/2fa/verify`, { userId, otpCode });
  }

  initiateTwoFactor(userId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/2fa/admin/initiate/${userId}`, {});
  }

  getPendingTwoFactor(userId: string): Observable<{ secret: string; qrCodeUrl: string; backupCodes: string[] }> {
    return this.http.get<{ secret: string; qrCodeUrl: string; backupCodes: string[] }>(
      `${this.apiUrl}/2fa/pending?userId=${userId}`
    );
  }

  enableTwoFactorByRH(userId: string): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>(`${this.apiUrl}/2fa/admin/enable/${userId}`, {});
  }

  enableTwoFactorForAll(): Observable<TwoFactorMassEnableResponse> {
    return this.http.post<TwoFactorMassEnableResponse>(`${this.apiUrl}/2fa/admin/enable-all`, {});
  }

  disableTwoFactor(userId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/2fa/admin/disable/${userId}`, {});
  }

  getTwoFactorStatus(userId: string): Observable<boolean> {
    return this.http.get<TwoFactorStatusResponse>(`${this.apiUrl}/2fa/status/${userId}`)
      .pipe(
        map(response => response.twoFactorEnabled)
      );
  }

  verifyBackupCode(userId: string, backupCode: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/2fa/verify-backup`, { userId, backupCode });
  }
}