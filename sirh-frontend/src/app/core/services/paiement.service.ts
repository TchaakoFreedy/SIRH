// src/app/core/services/paiement.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Paiement,
  PaiementRequest,
  SoldeEmploye
} from '../models/paiement.model';
import { Employee } from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class PaiementService {

  private url = `${environment.apiUrl}/paiements`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Paiement[]> {
    return this.http.get<Paiement[]>(this.url);
  }

  getByEmployee(employeeId: string): Observable<Paiement[]> {
    return this.http.get<Paiement[]>(`${this.url}/employee/${employeeId}`);
  }

  getByMois(mois: number, annee: number): Observable<Paiement[]> {
    const params = new HttpParams()
      .set('mois', mois)
      .set('annee', annee);
    return this.http.get<Paiement[]>(`${this.url}/mois`, { params });
  }

  getSolde(employeeId: string, mois: number, annee: number): Observable<SoldeEmploye> {
    const params = new HttpParams()
      .set('mois', mois)
      .set('annee', annee);
    return this.http.get<SoldeEmploye>(
      `${this.url}/solde/${employeeId}`,
      { params }
    );
  }

  getSoldes(mois: number, annee: number): Observable<SoldeEmploye[]> {
    const params = new HttpParams()
      .set('mois', mois)
      .set('annee', annee);
    return this.http.get<SoldeEmploye[]>(`${this.url}/soldes`, { params });
  }

  create(request: PaiementRequest): Observable<Paiement> {
    return this.http.post<Paiement>(this.url, request);
  }

  downloadRecu(paiementId: string): Observable<Blob> {
    return this.http.get(`${this.url}/${paiementId}/recu`, {
      responseType: 'blob'
    });
  }

  setSalaireMensuel(employeeId: string, salaireMensuel: number): Observable<Employee> {
    return this.http.patch<Employee>(
      `${this.url}/employee/${employeeId}/salaire`,
      { salaireMensuel }
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}