// src/app/features/cacao/services/acheteur.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Acheteur } from '../models/acheteur.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AcheteurService {
  private baseUrl = `${environment.apiUrl}/cacao/acheteurs`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Acheteur[]> {
    return this.http.get<Acheteur[]>(this.baseUrl);
  }

  getAllIncluantSuspendus(): Observable<Acheteur[]> {
    return this.http.get<Acheteur[]>(`${this.baseUrl}/all`);
  }

  getById(id: string): Observable<Acheteur> {
    return this.http.get<Acheteur>(`${this.baseUrl}/${id}`);
  }

  getByEmployeeId(employeeId: string): Observable<Acheteur> {
    return this.http.get<Acheteur>(`${this.baseUrl}/employee/${employeeId}`);
  }

  create(acheteur: Acheteur): Observable<Acheteur> {
    return this.http.post<Acheteur>(this.baseUrl, acheteur);
  }

  update(id: string, acheteur: Acheteur): Observable<Acheteur> {
    return this.http.put<Acheteur>(`${this.baseUrl}/${id}`, acheteur);
  }

  reactivate(id: string): Observable<Acheteur> {
    return this.http.patch<Acheteur>(`${this.baseUrl}/${id}/reactivate`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}