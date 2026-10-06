// src/app/core/services/employe.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employee } from '../models/employee.model';
import { Document } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class EmployeService {
  private url = `${environment.apiUrl}/api/employees`;
  private docUrl = `${environment.apiUrl}/api/documents-management`;
  private profileUrl = `${environment.apiUrl}/api/profile`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.url);
  }

  getById(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.url}/${id}`);
  }

  getByUserId(userId: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.url}/user/${userId}`);
  }

  findByMatricule(matricule: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.url}/matricule/${matricule}`);
  }

  getCurrentEmployee(): Observable<Employee> {
    return this.http.get<Employee>(`${this.url}/me`);
  }

  getByEntreprise(entrepriseId: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.url}/entreprise/${entrepriseId}`);
  }

  getMyCompanyEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.url}/my-company/employees`);
  }

  create(data: FormData): Observable<Employee> {
    return this.http.post<Employee>(this.url, data);
  }

  update(id: string, data: Partial<Employee>): Observable<Employee> {
    return this.http.patch<Employee>(`${this.url}/${id}`, data);
  }

  updateByAdmin(id: string, data: any): Observable<Employee> {
    return this.http.patch<Employee>(`${this.url}/${id}/admin`, data);
  }

  updateSelfProfile(id: string, data: { telephone: string; addresse: string }): Observable<Employee> {
    return this.http.patch<Employee>(`${this.url}/${id}/profile`, data);
  }

  updateMyProfile(data: { telephone: string; addresse: string; numeroContactUrgence?: string }): Observable<Employee> {
    return this.http.patch<Employee>(`${this.profileUrl}`, data);
  }

  getMyProfile(): Observable<Employee> {
    return this.http.get<Employee>(`${this.profileUrl}`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  suspendre(id: string): Observable<Employee> {
    return this.http.post<Employee>(`${this.url}/${id}/suspendre`, {});
  }

  reactiver(id: string): Observable<Employee> {
    return this.http.post<Employee>(`${this.url}/${id}/reactiver`, {});
  }

  search(term: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(
      `${this.url}/search?term=${encodeURIComponent(term)}`
    );
  }

  getByDepartement(departementId: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.url}/departement/${departementId}`);
  }

  getByPoste(posteId: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.url}/poste/${posteId}`);
  }

  getByStatut(statut: string): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.url}/statut/${statut}`);
  }

  changePassword(id: string, data: { ancienMotDePasse: string; nouveauMotDePasse: string }): Observable<any> {
    return this.http.post(`${this.url}/${id}/change-password`, data);
  }

  getPhoto(id: string): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/photo`, { responseType: 'blob' });
  }

  uploadPhoto(id: string, formData: FormData): Observable<Employee> {
    return this.http.post<Employee>(`${this.url}/${id}/photo`, formData);
  }

  getStats(id: string): Observable<any> {
    return this.http.get(`${this.url}/${id}/stats`);
  }

  getHistory(id: string): Observable<any> {
    return this.http.get<any>(`${this.url}/${id}/history`);
  }

  downloadHistory(employeeId: string, format: 'csv' | 'pdf' = 'csv'): Observable<Blob> {
    return this.http.get(`${this.url}/${employeeId}/history/download?format=${format}`, {
      responseType: 'blob'
    });
  }

  getEmployeeDocuments(employeeId: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.url}/${employeeId}/documents`);
  }

  getEmployeeContracts(employeeId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.url}/${employeeId}/contracts`);
  }

  uploadEmployeeDocument(
    employeeId: string,
    file: File,
    name: string,
    typeDocument: string
  ): Observable<Document> {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('typeDocument', typeDocument);
    formData.append('files', file);

    return this.http.post<Document>(
      `${this.docUrl}/pieces/employe/${employeeId}/upload`,
      formData
    );
  }

  uploadEmployeeDocuments(
    employeeId: string,
    files: File[],
    typeDocument: string
  ): Observable<Document[]> {
    const formData = new FormData();
    formData.append('employeeId', employeeId);
    formData.append('typeDocument', typeDocument);

    files.forEach(file => {
      formData.append('files', file);
    });

    return this.http.post<Document[]>(
      `${this.docUrl}/pieces/upload-multiple`,
      formData
    );
  }

  downloadDocument(documentId: string): Observable<Blob> {
    return this.http.get(`${this.docUrl}/pieces/${documentId}/file`, {
      responseType: 'blob'
    });
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.docUrl}/pieces/${documentId}`);
  }

  getEmployeeDocumentUrls(employeeId: string): Observable<{url: string, name: string}[]> {
    return this.http.get<{url: string, name: string}[]>(
      `${this.docUrl}/pieces/employe/${employeeId}/urls`
    );
  }

  uploadContractDocument(
    contratId: string,
    file: File,
    name: string,
    typeDocument: string
  ): Observable<Document> {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('typeDocument', typeDocument);
    formData.append('files', file);

    return this.http.post<Document>(
      `${this.docUrl}/pieces/contrat/${contratId}/upload`,
      formData
    );
  }
}