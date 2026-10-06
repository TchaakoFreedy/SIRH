// src/app/features/rh/paiements/paiements.component.ts

import {
  Component,
  OnInit,
  signal,
  computed,
  effect,
  inject,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, catchError, of, forkJoin } from 'rxjs';
import { PaiementService } from '../../../core/services/paiement.service';
import { EmployeService } from '../../../core/services/employe.service';
import { DepartementService } from '../../../core/services/departement.service';
import { PostesService } from '../../../core/services/postes.service';
import { PermissionService } from '../../../core/services/permission.service';
import { AuthService } from '../../../services/auth.service';
import { Paiement, PaiementRequest, SoldeEmploye, TypePaiement } from '../../../core/models/paiement.model';
import { Departement } from '../../../core/models/departement.model';
import { Poste } from '../../../core/models/poste.model';

interface PaiementFormState {
  employeeId: string;
  employeeLabel: string;
  type: TypePaiement;
  montant: number | null;
  motif: string;
  mois: number;
  annee: number;
  salaireMensuel: number;
  soldeRestant: number;
}

@Component({
  selector: 'app-paiements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './paiements.html',
  styleUrls: ['./paiements.component.css']
})
export class PaiementsComponent implements OnInit, OnDestroy {

  private paiementService = inject(PaiementService);
  private employeService = inject(EmployeService);
  private departementService = inject(DepartementService);
  private postesService = inject(PostesService);
  private permissionService = inject(PermissionService);
  private authService = inject(AuthService);

  private destroy$ = new Subject<void>();

  // ============================================
  // ETAT
  // ============================================

  employees = signal<any[]>([]);
  departements = signal<Departement[]>([]);
  postes = signal<Poste[]>([]);
  soldes = signal<SoldeEmploye[]>([]);
  paiements = signal<Paiement[]>([]);

  isLoading = signal<boolean>(false);
  loadingError = signal<string | null>(null);
  isSubmitting = signal<boolean>(false);
  isDownloading = signal<boolean>(false);

  // Filtres
  searchTerm = signal<string>('');
  selectedMois = signal<number>(new Date().getMonth() + 1);
  selectedAnnee = signal<number>(new Date().getFullYear());

  // Pagination
  currentPage = signal<number>(1);
  itemsPerPage = 10;
  Math = Math;

  // Modal
  showPaiementModal = signal<boolean>(false);
  showHistoriqueModal = signal<boolean>(false);
  showSalaireModal = signal<boolean>(false);

  historiqueEmployee = signal<any | null>(null);
  historiquePaiements = signal<Paiement[]>([]);
  isLoadingHistorique = signal<boolean>(false);

  salaireEmployee = signal<any | null>(null);
  salaireValue = signal<number | null>(null);

  form = signal<PaiementFormState>({
    employeeId: '',
    employeeLabel: '',
    type: 'PAIEMENT_SALAIRE',
    montant: null,
    motif: '',
    mois: new Date().getMonth() + 1,
    annee: new Date().getFullYear(),
    salaireMensuel: 0,
    soldeRestant: 0
  });

  formError = signal<string | null>(null);

  // Permissions
  canCreatePaiement = signal<boolean>(false);
  canDeletePaiement = signal<boolean>(false);
  canViewPaiements = signal<boolean>(false);

  // ============================================
  // COMPUTED
  // ============================================

  moisLabels = [
    'Janvier', 'Fevrier', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Aout', 'Septembre', 'Octobre', 'Novembre', 'Decembre'
  ];

  anneesDisponibles = computed(() => {
    const current = new Date().getFullYear();
    const list: number[] = [];
    for (let i = current - 3; i <= current + 1; i++) {
      list.push(i);
    }
    return list;
  });

  soldesEnrichis = computed(() => {
    const all = this.soldes();
    const employees = this.employees();
    const depts = this.departements();
    const postes = this.postes();

    return all.map(solde => {
      const emp = employees.find(e => e.id === solde.employeeId);
      const posteName = emp ? this.getPosteName(emp.posteId, postes) : solde.employeePoste;
      const deptName = emp
        ? this.getDepartementName(emp.departementId, depts)
        : solde.employeeDepartement;

      const statut = this.computeStatut(solde);

      return {
        ...solde,
        posteNom: posteName,
        departementNom: deptName,
        statut
      };
    });
  });

  filteredSoldes = computed(() => {
    const list = this.soldesEnrichis();
    const search = this.searchTerm().toLowerCase().trim();

    if (!search) {
      return list;
    }

    return list.filter(s => {
      const nom = (s.employeeNom || '').toLowerCase();
      const prenom = (s.employeePrenom || '').toLowerCase();
      const matricule = (s.employeeMatricule || '').toLowerCase();
      const poste = (s.posteNom || '').toLowerCase();
      const dept = (s.departementNom || '').toLowerCase();

      return nom.includes(search)
        || prenom.includes(search)
        || matricule.includes(search)
        || poste.includes(search)
        || dept.includes(search);
    });
  });

  totalPages = computed(() => {
    const total = this.filteredSoldes().length;
    return Math.max(1, Math.ceil(total / this.itemsPerPage));
  });

  paginatedSoldes = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredSoldes().slice(start, end);
  });

  // Stats
  statsTotal = computed(() => {
    const list = this.soldes();
    let totalSalaire = 0;
    let totalAvances = 0;
    let totalRetenues = 0;
    let totalPaye = 0;
    let totalRestant = 0;

    list.forEach(s => {
      totalSalaire += s.salaireMensuel || 0;
      totalAvances += s.totalAvances || 0;
      totalRetenues += s.totalRetenues || 0;
      totalPaye += s.totalPaye || 0;
      totalRestant += s.montantRestant || 0;
    });

    return {
      totalSalaire,
      totalAvances,
      totalRetenues,
      totalPaye,
      totalRestant
    };
  });

  // ============================================
  // LIFECYCLE
  // ============================================

  constructor() {
    effect(() => {
      this.filteredSoldes();
      this.currentPage.set(1);
    });
  }

  ngOnInit(): void {
    this.loadPermissions();
    this.loadInitialData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ============================================
  // CHARGEMENT
  // ============================================

  private loadPermissions(): void {
    const user = this.authService.getCurrentUser();
    const hasWildcard = user?.permissions?.includes('*') === true;
    const isSystemAdmin = this.permissionService.hasPermissionSync('SYSTEM_ADMIN');

    this.canCreatePaiement.set(
      hasWildcard
      || isSystemAdmin
      || this.permissionService.hasPermissionSync('PAYMENT_CREATE')
      || this.permissionService.hasPermissionSync('USER_UPDATE')
    );

    this.canDeletePaiement.set(
      hasWildcard
      || isSystemAdmin
      || this.permissionService.hasPermissionSync('PAYMENT_DELETE')
      || this.permissionService.hasPermissionSync('USER_DELETE')
    );

    this.canViewPaiements.set(
      hasWildcard
      || isSystemAdmin
      || this.permissionService.hasPermissionSync('PAYMENT_VIEW')
      || this.permissionService.hasPermissionSync('EMPLOYEE_VIEW')
    );
  }

  private loadInitialData(): void {
    this.isLoading.set(true);
    this.loadingError.set(null);

    forkJoin({
      employees: this.employeService.getAll().pipe(catchError(() => of([]))),
      departements: this.departementService.getAll().pipe(catchError(() => of([]))),
      postes: this.postesService.getAll().pipe(catchError(() => of([]))),
      paiements: this.paiementService.getAll().pipe(catchError(() => of([])))
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          this.employees.set(data.employees || []);
          this.departements.set(data.departements || []);
          this.postes.set(data.postes || []);
          this.paiements.set(data.paiements || []);
          this.loadSoldes();
        },
        error: (err: any) => {
          console.error('Erreur chargement donnees:', err);
          this.loadingError.set('Erreur lors du chargement des donnees.');
          this.isLoading.set(false);
        }
      });
  }

  loadSoldes(): void {
    this.isLoading.set(true);
    this.paiementService
      .getSoldes(this.selectedMois(), this.selectedAnnee())
      .pipe(
        catchError(() => of([])),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (soldes: SoldeEmploye[]) => {
          this.soldes.set(soldes || []);
          this.isLoading.set(false);
        },
        error: (err: any) => {
          console.error('Erreur chargement soldes:', err);
          this.soldes.set([]);
          this.isLoading.set(false);
        }
      });
  }

  reloadAll(): void {
    this.paiementService
      .getAll()
      .pipe(catchError(() => of([])), takeUntil(this.destroy$))
      .subscribe((p: Paiement[]) => this.paiements.set(p));
    this.loadSoldes();
  }

  // ============================================
  // FILTRES
  // ============================================

  onMoisChange(value: any): void {
    this.selectedMois.set(this.toNumber(value));
    this.loadSoldes();
  }

  onAnneeChange(value: any): void {
    this.selectedAnnee.set(this.toNumber(value));
    this.loadSoldes();
  }

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.selectedMois.set(new Date().getMonth() + 1);
    this.selectedAnnee.set(new Date().getFullYear());
    this.loadSoldes();
  }

  // ============================================
  // PAGINATION
  // ============================================

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }

  getVisiblePages(): number[] {
    const current = this.currentPage();
    const total = this.totalPages();
    const pages: number[] = [];
    const delta = 1;
    for (
      let i = Math.max(2, current - delta);
      i <= Math.min(total - 1, current + delta);
      i++
    ) {
      pages.push(i);
    }
    return pages;
  }

  onPageSizeChange(value: any): void {
    this.itemsPerPage = this.toNumber(value);
    this.currentPage.set(1);
  }

  // ============================================
  // ACTIONS PAIEMENT
  // ============================================

  openPayerModal(solde: any): void {
    this.openModal(solde, 'PAIEMENT_SALAIRE');
  }

  openAvanceModal(solde: any): void {
    this.openModal(solde, 'AVANCE');
  }

  openRetenueModal(solde: any): void {
    this.openModal(solde, 'RETENUE');
  }

  private openModal(solde: any, type: TypePaiement): void {
    if (!this.canCreatePaiement()) {
      alert('Vous n\'avez pas la permission d\'effectuer un paiement.');
      return;
    }

    if (!solde.salaireConfigure) {
      if (confirm(
        'Le salaire mensuel de cet employe n\'est pas configure. Voulez-vous le configurer maintenant ?'
      )) {
        this.openSalaireModal(solde);
      }
      return;
    }

    if (type === 'PAIEMENT_SALAIRE' && solde.montantRestant <= 0) {
      alert('Le salaire de cet employe est deja entierement paye pour cette periode.');
      return;
    }

    this.formError.set(null);
    this.form.set({
      employeeId: solde.employeeId,
      employeeLabel: `${solde.employeePrenom || ''} ${solde.employeeNom || ''}`.trim(),
      type,
      montant: type === 'PAIEMENT_SALAIRE' ? solde.montantRestant : null,
      motif: '',
      mois: this.selectedMois(),
      annee: this.selectedAnnee(),
      salaireMensuel: solde.salaireMensuel,
      soldeRestant: solde.montantRestant
    });
    this.showPaiementModal.set(true);
  }

  closePaiementModal(): void {
    this.showPaiementModal.set(false);
    this.formError.set(null);
  }

  onChangeFormType(value: any): void {
    this.form.set({ ...this.form(), type: value as TypePaiement });
  }

  onChangeFormMontant(value: any): void {
    const num = value === null || value === '' ? null : this.toNumber(value);
    this.form.set({ ...this.form(), montant: num });
  }

  onChangeFormMotif(value: any): void {
    this.form.set({ ...this.form(), motif: value });
  }

  onChangeFormMois(value: any): void {
    this.form.set({ ...this.form(), mois: this.toNumber(value) });
  }

  onChangeFormAnnee(value: any): void {
    this.form.set({ ...this.form(), annee: this.toNumber(value) });
  }

  submitPaiement(): void {
    const data = this.form();

    if (!data.montant || data.montant <= 0) {
      this.formError.set('Veuillez saisir un montant valide.');
      return;
    }

    if (data.type === 'RETENUE' && (!data.motif || !data.motif.trim())) {
      this.formError.set('Le motif est obligatoire pour une retenue.');
      return;
    }

    if (
      (data.type === 'PAIEMENT_SALAIRE' || data.type === 'AVANCE')
      && data.montant > data.soldeRestant + 0.001
    ) {
      this.formError.set(
        `Le montant depasse le solde restant (${this.formatMoney(data.soldeRestant)}).`
      );
      return;
    }

    const request: PaiementRequest = {
      employeeId: data.employeeId,
      type: data.type,
      montant: data.montant,
      motif: data.motif || undefined,
      mois: data.mois,
      annee: data.annee
    };

    this.isSubmitting.set(true);

    this.paiementService.create(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (paiement: Paiement) => {
          this.isSubmitting.set(false);
          this.closePaiementModal();
          this.reloadAll();
          alert(
            'Paiement enregistre avec succes. Numero de recu : '
            + (paiement.numeroRecu || '')
          );
        },
        error: (err: any) => {
          this.isSubmitting.set(false);
          const message = err?.error?.message
            || err?.error?.error
            || err?.message
            || 'Erreur lors de l\'enregistrement.';
          this.formError.set(message);
        }
      });
  }

  // ============================================
  // HISTORIQUE
  // ============================================

  openHistoriqueModal(solde: any): void {
    this.historiqueEmployee.set(solde);
    this.historiquePaiements.set([]);
    this.isLoadingHistorique.set(true);
    this.showHistoriqueModal.set(true);

    this.paiementService.getByEmployee(solde.employeeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (paiements: Paiement[]) => {
          this.historiquePaiements.set(paiements || []);
          this.isLoadingHistorique.set(false);
        },
        error: () => {
          this.historiquePaiements.set([]);
          this.isLoadingHistorique.set(false);
        }
      });
  }

  closeHistoriqueModal(): void {
    this.showHistoriqueModal.set(false);
    this.historiqueEmployee.set(null);
    this.historiquePaiements.set([]);
  }

  // ============================================
  // SALAIRE MENSUEL
  // ============================================

  openSalaireModal(solde: any): void {
    if (!this.canCreatePaiement()) {
      alert('Vous n\'avez pas la permission de modifier le salaire.');
      return;
    }
    this.salaireEmployee.set(solde);
    this.salaireValue.set(solde.salaireMensuel > 0 ? solde.salaireMensuel : null);
    this.showSalaireModal.set(true);
  }

  closeSalaireModal(): void {
    this.showSalaireModal.set(false);
    this.salaireEmployee.set(null);
    this.salaireValue.set(null);
  }

  onChangeSalaireValue(value: any): void {
    const num = value === null || value === '' ? null : this.toNumber(value);
    this.salaireValue.set(num);
  }

  submitSalaire(): void {
    const value = this.salaireValue();
    const emp = this.salaireEmployee();
    if (!emp) return;

    if (value === null || value <= 0) {
      alert('Veuillez saisir un salaire valide.');
      return;
    }

    this.isSubmitting.set(true);

    this.paiementService.setSalaireMensuel(emp.employeeId, value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeSalaireModal();
          this.reloadAll();
          alert('Salaire mis a jour avec succes.');
        },
        error: (err: any) => {
          this.isSubmitting.set(false);
          alert('Erreur : ' + (err?.error?.message || err?.message || 'inconnue'));
        }
      });
  }

  // ============================================
  // RECU
  // ============================================

  downloadRecu(paiement: Paiement): void {
    if (!paiement.id) return;

    this.isDownloading.set(true);

    this.paiementService.downloadRecu(paiement.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `recu_${paiement.numeroRecu || paiement.id}.pdf`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
          this.isDownloading.set(false);
        },
        error: (err: any) => {
          console.error('Erreur telechargement recu:', err);
          this.isDownloading.set(false);
          alert('Impossible de telecharger le recu.');
        }
      });
  }

  // ============================================
  // SUPPRESSION
  // ============================================

  deletePaiement(paiement: Paiement): void {
    if (!this.canDeletePaiement()) {
      alert('Vous n\'avez pas la permission de supprimer un paiement.');
      return;
    }
    if (!paiement.id) return;

    if (!confirm(
      'Supprimer ce paiement ? Cette action est irreversible.'
    )) {
      return;
    }

    this.paiementService.delete(paiement.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.reloadAll();
          if (this.showHistoriqueModal()) {
            const emp = this.historiqueEmployee();
            if (emp) {
              this.openHistoriqueModal(emp);
            }
          }
        },
        error: () => alert('Erreur lors de la suppression.')
      });
  }

  // ============================================
  // HELPERS
  // ============================================

  /**
   * Convertit une valeur (issue d'un <select> ou <input>) en nombre.
   * Expose cette methode au template car Number n'y est pas accessible.
   */
  toNumber(value: any): number {
    if (value === null || value === undefined || value === '') {
      return 0;
    }
    const num = Number(value);
    return isNaN(num) ? 0 : num;
  }

  private computeStatut(solde: SoldeEmploye): 'PAYE' | 'PARTIEL' | 'EN_ATTENTE' | 'NON_CONFIGURE' {
    if (!solde.salaireConfigure) return 'NON_CONFIGURE';
    if (solde.montantRestant <= 0 && solde.montantNetAPayer > 0) return 'PAYE';
    if (solde.totalPaye > 0) return 'PARTIEL';
    return 'EN_ATTENTE';
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'Paye';
      case 'PARTIEL': return 'Partiel';
      case 'EN_ATTENTE': return 'En attente';
      case 'NON_CONFIGURE': return 'Salaire non configure';
      default: return statut;
    }
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'PAYE': return 'active';
      case 'PARTIEL': return 'partial';
      case 'EN_ATTENTE': return 'pending';
      case 'NON_CONFIGURE': return 'inactive';
      default: return '';
    }
  }

  getTypeLabel(type: TypePaiement): string {
    switch (type) {
      case 'PAIEMENT_SALAIRE': return 'Paiement salaire';
      case 'AVANCE': return 'Avance';
      case 'RETENUE': return 'Retenue';
      default: return type;
    }
  }

  getPosteName(posteId: string, postes: Poste[]): string {
    if (!posteId) return 'Non defini';
    const p = postes.find((x: Poste) => x.id === posteId || x.code === posteId);
    return p ? (p.libelle || p.code || 'Poste') : 'Poste';
  }

  getDepartementName(deptId: string, depts: Departement[]): string {
    if (!deptId) return 'Non assigne';
    const d = depts.find(x => x.id === deptId);
    return d ? (d.name || 'Non assigne') : 'Non assigne';
  }

  formatMoney(value: number | undefined | null): string {
    if (value === null || value === undefined) return '0 FCFA';
    const rounded = Math.round(value);
    return rounded.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ') + ' FCFA';
  }

  formatDate(value: string | undefined | null): string {
    if (!value) return '-';
    try {
      const d = new Date(value);
      const pad = (n: number) => n < 10 ? '0' + n : '' + n;
      return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear()
        + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    } catch {
      return value;
    }
  }

  getInitials(nom?: string, prenom?: string): string {
    const n = (nom || '').charAt(0).toUpperCase();
    const p = (prenom || '').charAt(0).toUpperCase();
    return (p + n) || 'E';
  }
}