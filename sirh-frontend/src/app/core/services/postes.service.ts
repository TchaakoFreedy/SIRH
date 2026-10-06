import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, catchError, retry, map } from 'rxjs';
import { Poste, CreatePosteRequest, UpdatePosteRequest } from '../models/poste.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PostesService {
  private apiUrl = `${environment.apiUrl}/api/postes`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Poste[]> {
    return this.http.get<Poste[]>(this.apiUrl).pipe(
      retry(1),
      catchError(this.handleError)
    );
  }

  getActive(): Observable<Poste[]> {
    return this.http.get<Poste[]>(`${this.apiUrl}/active`).pipe(
      retry(1),
      catchError(this.handleError)
    );
  }

  getById(id: string): Observable<Poste> {
    return this.http.get<Poste>(`${this.apiUrl}/${id}`).pipe(
      retry(1),
      catchError(this.handleError)
    );
  }

  create(poste: CreatePosteRequest): Observable<Poste> {
    return this.http.post<Poste>(this.apiUrl, poste).pipe(
      catchError(this.handleError)
    );
  }

  update(id: string, poste: UpdatePosteRequest): Observable<Poste> {
    return this.http.put<Poste>(`${this.apiUrl}/${id}`, poste).pipe(
      catchError(this.handleError)
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  toggleActive(id: string): Observable<Poste> {
    return this.http.patch<Poste>(`${this.apiUrl}/${id}/toggle`, {}).pipe(
      catchError(this.handleError)
    );
  }

  getByDepartement(departementId: string): Observable<Poste[]> {
    return this.http.get<Poste[]>(`${this.apiUrl}/departement/${departementId}`).pipe(
      retry(1),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur inattendue est survenue';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur réseau: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de se connecter au serveur';
          break;
        case 400:
          errorMessage = error.error?.message || 'Requête invalide';
          break;
        case 401:
          errorMessage = 'Non autorisé - Veuillez vous reconnecter';
          break;
        case 403:
          errorMessage = 'Accès interdit - Vous n\'avez pas les droits nécessaires';
          break;
        case 404:
          errorMessage = error.error?.message || 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflit - La ressource existe déjà';
          break;
        case 500:
          errorMessage = 'Erreur interne du serveur';
          break;
        default:
          errorMessage = error.error?.message || `Erreur ${error.status}: ${error.statusText}`;
      }
    }

    console.error('Erreur API:', {
      status: error.status,
      message: errorMessage,
      details: error.error
    });

    return throwError(() => ({
      status: error.status,
      message: errorMessage,
      originalError: error
    }));
  }
}