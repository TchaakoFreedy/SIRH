// src/app/core/services/contract-alert-config.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContractAlertConfig,
  UpdateContractAlertConfigRequest
} from '../models/contrat.model';

@Injectable({ providedIn: 'root' })
export class ContractAlertConfigService {

  // ✅ On ajoute /api explicitement, comme les autres services
  // (environment.apiUrl reste inchangé = https://sirh-production-f9dd.up.railway.app)
  private baseUrl = `${environment.apiUrl}/api/contract-alert-configs`;

  constructor(private http: HttpClient) {}

  getConfig(): Observable<ContractAlertConfig> {
    return this.http.get<ContractAlertConfig>(`${this.baseUrl}/global`);
  }

  updateConfig(request: UpdateContractAlertConfigRequest): Observable<ContractAlertConfig> {
    return this.http.put<ContractAlertConfig>(`${this.baseUrl}/global`, request);
  }
}