// src/app/features/cacao/services/reception.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReceptionCacao } from '../models/reception-cacao.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReceptionService {
  private baseUrl = `${environment.apiUrl}/cacao/receptions`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ReceptionCacao[]> {
    return this.http.get<ReceptionCacao[]>(this.baseUrl);
  }

  getByAcheteur(acheteurId: string): Observable<ReceptionCacao[]> {
    if (!acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour recuperer les receptions.');
    }
    return this.http.get<ReceptionCacao[]>(`${this.baseUrl}/acheteur/${acheteurId}`);
  }

  getNonRembourseesByAcheteur(acheteurId: string): Observable<ReceptionCacao[]> {
    if (!acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour recuperer les receptions non remboursees.');
    }
    return this.http.get<ReceptionCacao[]>(`${this.baseUrl}/non-remboursees/acheteur/${acheteurId}`);
  }

  getById(id: string): Observable<ReceptionCacao> {
    if (!id) {
      throw new Error('L\'ID de la reception est requis');
    }
    return this.http.get<ReceptionCacao>(`${this.baseUrl}/${id}`);
  }

  create(reception: ReceptionCacao): Observable<ReceptionCacao> {
    if (!reception.acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour creer une reception');
    }
    return this.http.post<ReceptionCacao>(this.baseUrl, reception);
  }

  delete(id: string): Observable<void> {
    if (!id) {
      throw new Error('L\'ID de la reception est requis pour la suppression');
    }
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}