// src/app/core/services/entreprise.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Entreprise, EntreprisePayload } from '../models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EntrepriseService {
  private apiUrl = `${environment.apiUrl}/api/entreprises`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(this.apiUrl);
  }

  getById(id: string): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${this.apiUrl}/${id}`);
  }

  getByEmployeeId(employeeId: string): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${this.apiUrl}/by-employee/${employeeId}`);
  }

  getMyEntreprise(employeeId: string): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${this.apiUrl}/my-entreprise?employeeId=${employeeId}`);
  }

  create(payload: EntreprisePayload): Observable<Entreprise> {
    return this.http.post<Entreprise>(this.apiUrl, payload);
  }

  update(id: string, payload: EntreprisePayload): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}`, payload);
  }

  suspendre(id: string): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}/suspendre`, {});
  }

  reactiver(id: string): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}/reactiver`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}