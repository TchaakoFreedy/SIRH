// src/app/core/services/user.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  password?: string;
  active: boolean;
  roleId: string;
  loginAttempts: number;
  locked: boolean;
  createdAt: string;
  createdBy: string;
  role?: Role;
  employeeId?: string;
}

export interface Role {
  _id: string;
  name: string;
  description: string;
  hierarchyLevel: number;
  permissionIds: string[];
  visibilityScope: 'TEAM' | 'COMPANY' | 'GROUP' | 'ALL' | 'SELF';
  active: boolean;
  createdAt: string;
  createdBy: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api`;

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  getUser(id: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/${id}`).pipe(
      tap(user => {
        if (user.roleId) {
          this.getRole(user.roleId).subscribe(role => {
            user.role = role;
            this.currentUserSubject.next(user);
          });
        } else {
          this.currentUserSubject.next(user);
        }
      })
    );
  }

  getRole(roleId: string): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/roles/${roleId}`);
  }

  updateUser(id: string, userData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/${id}`, userData);
  }

  toggleUserStatus(id: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/users/${id}/toggle-status`, {});
  }

  getCurrentUser(): User | null {
    const stored = localStorage.getItem('currentUser');
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return null;
      }
    }
    return null;
  }

  setCurrentUser(user: User): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  getFullName(user: User): string {
    return `${user.firstName} ${user.lastName}`;
  }

  getInitials(user: User): string {
    if (!user) return 'UT';
    const first = user.firstName ? user.firstName.charAt(0) : '';
    const last = user.lastName ? user.lastName.charAt(0) : '';
    return (first + last).toUpperCase() || 'UT';
  }

  getRoleLabel(roleName: string): string {
    const roleMap: { [key: string]: string } = {
      'SUPER_ADMIN': 'Administrateur',
      'RH': 'Ressources Humaines',
      'MANAGER': 'Manager',
      'EMPLOY': 'Employé',
      'DIRECTION': 'Direction',
      'TOP_MANAGER': 'Top Management'
    };
    return roleMap[roleName] || roleName || 'Utilisateur';
  }

  getRoleColor(roleName: string): string {
    const colorMap: { [key: string]: string } = {
      'SUPER_ADMIN': '#8b5cf6',
      'RH': '#0d9488',
      'MANAGER': '#3b82f6',
      'EMPLOY': '#6b7280',
      'DIRECTION': '#f59e0b',
      'TOP_MANAGER': '#ef4444'
    };
    return colorMap[roleName] || '#6b7280';
  }
}