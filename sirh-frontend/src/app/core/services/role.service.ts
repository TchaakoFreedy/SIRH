// src/app/core/services/role.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Role {
  id: string;
  name: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private apiUrl = `${environment.apiUrl}/api/roles`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Role[]> {
    return this.http.get<Role[]>(this.apiUrl);
  }
}