// src/app/features/cacao/services/remboursement.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Remboursement } from '../models/remboursement.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RemboursementService {
  // ✅ /api ajouté ici
  private baseUrl = `${environment.apiUrl}/api/cacao/remboursements`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Remboursement[]> {
    return this.http.get<Remboursement[]>(this.baseUrl);
  }

  getByAcheteur(acheteurId: string): Observable<Remboursement[]> {
    if (!acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour recuperer les remboursements.');
    }
    return this.http.get<Remboursement[]>(`${this.baseUrl}/acheteur/${acheteurId}`);
  }

  getById(id: string): Observable<Remboursement> {
    if (!id) {
      throw new Error('L\'ID du remboursement est requis');
    }
    return this.http.get<Remboursement>(`${this.baseUrl}/${id}`);
  }

  create(remboursement: Remboursement): Observable<Remboursement> {
    if (!remboursement.acheteurId) {
      throw new Error('L\'ID de l\'acheteur est requis pour creer un remboursement');
    }
    if (!remboursement.receptionId) {
      throw new Error('L\'ID de la reception est requis pour creer un remboursement');
    }
    return this.http.post<Remboursement>(this.baseUrl, remboursement);
  }

  delete(id: string): Observable<void> {
    if (!id) {
      throw new Error('L\'ID du remboursement est requis pour la suppression');
    }
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}