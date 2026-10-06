// src/app/features/cacao/services/avance.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AvanceFinanciere } from '../models/avance-financiere.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AvanceService {
  private baseUrl = `${environment.apiUrl}/cacao/avances`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<AvanceFinanciere[]> {
    return this.http.get<AvanceFinanciere[]>(this.baseUrl);
  }

  getByAcheteur(acheteurId: string): Observable<AvanceFinanciere[]> {
    if (!acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour recuperer les avances.');
    }
    return this.http.get<AvanceFinanciere[]>(`${this.baseUrl}/acheteur/${acheteurId}`);
  }

  getById(id: string): Observable<AvanceFinanciere> {
    if (!id) {
      throw new Error('L\'ID de l\'avance est requis');
    }
    return this.http.get<AvanceFinanciere>(`${this.baseUrl}/${id}`);
  }

  create(avance: AvanceFinanciere): Observable<AvanceFinanciere> {
    if (!avance.acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour creer une avance');
    }
    return this.http.post<AvanceFinanciere>(this.baseUrl, avance);
  }

  delete(id: string): Observable<void> {
    if (!id) {
      throw new Error('L\'ID de l\'avance est requis pour la suppression');
    }
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}