// src/app/features/cacao/components/avance-list/avance-list.component.ts

import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AvanceService } from '../../services/avance.service';
import { AcheteurService } from '../../services/acheteur.service';
import { AvanceFinanciere } from '../../models/avance-financiere.model';
import { Acheteur } from '../../models/acheteur.model';

@Component({
  selector: 'app-avance-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './avance-list.component.html',
  styleUrls: ['./avance-list.component.scss']
})
export class AvanceListComponent implements OnInit {
  private avanceService = inject(AvanceService);
  private acheteurService = inject(AcheteurService);

  avances = signal<AvanceFinanciere[]>([]);
  acheteurs = signal<Acheteur[]>([]);
  loading = signal(false);
  error = signal('');

  searchTerm = signal('');
  selectedAcheteurId = signal('');
  currentPage = signal(1);
  itemsPerPage = signal(10);
  Math = Math;

  get filteredAvances(): AvanceFinanciere[] {
    const term = this.searchTerm().toLowerCase().trim();
    const acheteurId = this.selectedAcheteurId();

    return this.avances().filter(a => {
      const matchTerm = !term ||
        a.acheteurNomComplet?.toLowerCase().includes(term) ||
        a.motif?.toLowerCase().includes(term) ||
        a.modePaiement?.toLowerCase().includes(term);

      const matchAcheteur = !acheteurId || a.acheteurId === acheteurId;

      return matchTerm && matchAcheteur;
    });
  }

  get paginatedAvances(): AvanceFinanciere[] {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filteredAvances.slice(start, start + this.itemsPerPage());
  }

  get totalPages(): number {
    return Math.ceil(this.filteredAvances.length / this.itemsPerPage());
  }

  get totalAvances(): number {
    return this.filteredAvances.reduce((sum, a) => sum + (a.montant || 0), 0);
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.avanceService.getAll().subscribe({
      next: (data) => {
        this.avances.set(data);
        this.loadAcheteurs();
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des avances');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  loadAcheteurs(): void {
    this.acheteurService.getAll().subscribe({
      next: (data) => {
        this.acheteurs.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement acheteurs', err);
        this.loading.set(false);
      }
    });
  }

  loadAvancesByAcheteur(acheteurId: string): void {
    if (!acheteurId) {
      this.loadData();
      return;
    }

    this.loading.set(true);
    this.avanceService.getByAcheteur(acheteurId).subscribe({
      next: (data) => {
        this.avances.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des avances pour cet acheteur');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  deleteAvance(id: string | undefined): void {
    if (!id) return;
    if (confirm('Voulez-vous vraiment supprimer cette avance ?')) {
      this.avanceService.delete(id).subscribe({
        next: () => {
          const acheteurId = this.selectedAcheteurId();
          if (acheteurId) {
            this.loadAvancesByAcheteur(acheteurId);
          } else {
            this.loadData();
          }
        },
        error: (err) => {
          this.error.set('Erreur lors de la suppression');
          console.error(err);
        }
      });
    }
  }

  onAcheteurFilterChange(acheteurId: string): void {
    this.selectedAcheteurId.set(acheteurId);
    this.currentPage.set(1);
    if (acheteurId) {
      this.loadAvancesByAcheteur(acheteurId);
    } else {
      this.loadData();
    }
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  clearFilter(): void {
    this.selectedAcheteurId.set('');
    this.currentPage.set(1);
    this.loadData();
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.selectedAcheteurId.set('');
    this.currentPage.set(1);
    this.loadData();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage.set(page);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }

  onPageSizeChange(): void {
    this.currentPage.set(1);
  }

  getVisiblePages(): number[] {
    const total = this.totalPages;
    const current = this.currentPage();
    const pages: number[] = [];

    if (total <= 5) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      if (current <= 3) {
        for (let i = 1; i <= 5; i++) pages.push(i);
      } else if (current >= total - 2) {
        for (let i = total - 4; i <= total; i++) pages.push(i);
      } else {
        for (let i = current - 2; i <= current + 2; i++) pages.push(i);
      }
    }
    return pages;
  }
}