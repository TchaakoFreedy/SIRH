// src/app/features/cacao/components/acheteur-form/acheteur-form.component.ts

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AcheteurService } from '../../services/acheteur.service';
import { Acheteur } from '../../models/acheteur.model';
import { EmployeService } from '../../../../core/services/employe.service';
import { Employee } from '../../../../core/models/employee.model';

@Component({
  selector: 'app-acheteur-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acheteur-form.component.html',
  styleUrls: ['./acheteur-form.component.scss']
})
export class AcheteurFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private acheteurService = inject(AcheteurService);
  private employeService = inject(EmployeService);

  isEditMode = signal(false);
  loading = signal(false);
  error = signal('');
  successMessage = signal('');
  isReactivating = signal(false);
  suspendedAcheteurId = signal<string | null>(null);
  suspendedAcheteurNom = signal<string | null>(null);

  acheteur = signal<Acheteur>({
    employeeId: '',
    zoneCollecte: ''
  });

  employees = signal<Employee[]>([]);
  loadingEmployees = signal(false);
  employeeStatusMap = signal<Map<string, { exists: boolean; statut: string; id: string; nom: string }>>(new Map());

  ngOnInit(): void {
    this.loadEmployees();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.loadAcheteur(id);
    }
  }

  loadEmployees(): void {
    this.loadingEmployees.set(true);
    this.employeService.getAll().subscribe({
      next: (data) => {
        this.employees.set(data);
        this.loadingEmployees.set(false);
        this.loadAcheteurStatuses();
      },
      error: (err) => {
        console.error('Erreur chargement employes', err);
        this.loadingEmployees.set(false);
        this.error.set('Erreur lors du chargement des employes');
      }
    });
  }

  loadAcheteurStatuses(): void {
    this.acheteurService.getAllIncluantSuspendus().subscribe({
      next: (acheteurs) => {
        const statusMap = new Map<string, { exists: boolean; statut: string; id: string; nom: string }>();
        acheteurs.forEach(a => {
          if (a.employeeId) {
            statusMap.set(a.employeeId, {
              exists: true,
              statut: a.statut || 'INACTIF',
              id: a.id || '',
              nom: a.nomComplet || 'Employe inconnu'
            });
          }
        });
        this.employeeStatusMap.set(statusMap);
      },
      error: (err) => {
        console.error('Erreur chargement statuts acheteurs', err);
      }
    });
  }

  loadAcheteur(id: string): void {
    this.loading.set(true);
    this.acheteurService.getById(id).subscribe({
      next: (data) => {
        this.acheteur.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur chargement acheteur');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  getEmployeeStatus(employeeId: string): string {
    const status = this.employeeStatusMap().get(employeeId);
    if (!status) return 'Disponible';
    if (status.statut === 'ACTIF') return 'Deja acheteur actif';
    if (status.statut === 'SUSPENDU') return 'Acheteur suspendu - Reactivable';
    return 'Statut inconnu';
  }

  getEmployeeStatusClass(employeeId: string): string {
    const status = this.employeeStatusMap().get(employeeId);
    if (!status) return 'status-available';
    if (status.statut === 'ACTIF') return 'status-active';
    if (status.statut === 'SUSPENDU') return 'status-suspended';
    return 'status-unknown';
  }

  isEmployeeActiveAcheteur(employeeId: string): boolean {
    const status = this.employeeStatusMap().get(employeeId);
    return status ? status.statut === 'ACTIF' : false;
  }

  isEmployeeSuspendedAcheteur(employeeId: string): boolean {
    const status = this.employeeStatusMap().get(employeeId);
    return status ? status.statut === 'SUSPENDU' : false;
  }

  getSuspendedAcheteurId(employeeId: string): string | null {
    const status = this.employeeStatusMap().get(employeeId);
    return status && status.statut === 'SUSPENDU' ? status.id : null;
  }

  getSuspendedAcheteurNom(employeeId: string): string | null {
    const status = this.employeeStatusMap().get(employeeId);
    return status && status.statut === 'SUSPENDU' ? status.nom : null;
  }

  getEmployeeDisplayName(employee: Employee): string {
    const prenom = employee.prenom || '';
    const nom = employee.nom || '';
    const matricule = employee.matriculeInterne || '';
    const status = this.getEmployeeStatus(employee.id || '');
    return `${prenom} ${nom} - ${matricule}`;
  }

  getEmployeeStatusBadge(employeeId: string): string {
    const status = this.employeeStatusMap().get(employeeId);
    if (!status) return '';
    if (status.statut === 'ACTIF') return ' (ACTIF)';
    if (status.statut === 'SUSPENDU') return ' (SUSPENDU)';
    return '';
  }

  isEmployeeSelectable(employeeId: string): boolean {
    return !this.isEmployeeActiveAcheteur(employeeId);
  }

  onEmployeeSelect(employeeId: string): void {
    this.acheteur.set({ ...this.acheteur(), employeeId: employeeId });
    this.error.set('');
    this.successMessage.set('');
    this.isReactivating.set(false);
    this.suspendedAcheteurId.set(null);
    this.suspendedAcheteurNom.set(null);

    if (this.isEmployeeSuspendedAcheteur(employeeId)) {
      const suspendedId = this.getSuspendedAcheteurId(employeeId);
      const suspendedNom = this.getSuspendedAcheteurNom(employeeId);
      if (suspendedId) {
        this.suspendedAcheteurId.set(suspendedId);
        this.suspendedAcheteurNom.set(suspendedNom);
        this.isReactivating.set(true);
        this.successMessage.set(`L'employe ${suspendedNom || ''} a un compte acheteur suspendu. Cliquez sur "Reactivar" pour le reactiver.`);
      }
    }
  }

  onSubmit(): void {
    const currentAcheteur = this.acheteur();
    if (!currentAcheteur.employeeId) {
      this.error.set('Veuillez selectionner un employe');
      return;
    }

    if (this.isEmployeeActiveAcheteur(currentAcheteur.employeeId)) {
      this.error.set('Cet employe est deja enregistre comme acheteur actif');
      return;
    }

    // Si l'employe a un compte suspendu, on propose la reactivation
    if (this.isReactivating() && this.suspendedAcheteurId()) {
      this.reactivateAcheteur(this.suspendedAcheteurId()!);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.successMessage.set('');

    this.acheteurService.create(currentAcheteur).subscribe({
      next: (result) => {
        this.loading.set(false);
        const message = this.isReactivating() ? 'Acheteur reactive avec succes' : 'Acheteur cree avec succes';
        this.successMessage.set(message);
        setTimeout(() => {
          this.router.navigate(['/app/cacao/acheteurs']);
        }, 1500);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.error && err.error.message) {
          this.error.set(err.error.message);
        } else {
          this.error.set('Erreur lors de l\'enregistrement de l\'acheteur');
        }
        console.error(err);
      }
    });
  }

  reactivateAcheteur(id: string): void {
    this.loading.set(true);
    this.error.set('');
    this.successMessage.set('');

    this.acheteurService.reactivate(id).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.successMessage.set('Acheteur reactive avec succes');
        setTimeout(() => {
          this.router.navigate(['/app/cacao/acheteurs']);
        }, 1500);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.error && err.error.message) {
          this.error.set(err.error.message);
        } else {
          this.error.set('Erreur lors de la reactivation de l\'acheteur');
        }
        console.error(err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/app/cacao/acheteurs']);
  }
}