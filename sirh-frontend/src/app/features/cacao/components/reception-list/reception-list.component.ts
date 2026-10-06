// src/app/features/cacao/components/reception-list/reception-list.component.ts

import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ReceptionService } from '../../services/reception.service';
import { AcheteurService } from '../../services/acheteur.service';
import { ReceptionCacao } from '../../models/reception-cacao.model';
import { Acheteur } from '../../models/acheteur.model';

@Component({
  selector: 'app-reception-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reception-list.component.html',
  styleUrls: ['./reception-list.component.scss']
})
export class ReceptionListComponent implements OnInit {
  private receptionService = inject(ReceptionService);
  private acheteurService = inject(AcheteurService);

  receptions = signal<ReceptionCacao[]>([]);
  acheteurs = signal<Acheteur[]>([]);
  loading = signal(false);
  error = signal('');

  searchTerm = signal('');
  selectedAcheteurId = signal('');
  currentPage = signal(1);
  itemsPerPage = signal(10);
  Math = Math;

  get filteredReceptions(): ReceptionCacao[] {
    const term = this.searchTerm().toLowerCase().trim();
    const acheteurId = this.selectedAcheteurId();

    return this.receptions().filter(r => {
      const matchTerm = !term ||
        r.acheteurNomComplet?.toLowerCase().includes(term) ||
        r.qualite?.toLowerCase().includes(term) ||
        r.numBonReception?.toLowerCase().includes(term);

      const matchAcheteur = !acheteurId || r.acheteurId === acheteurId;

      return matchTerm && matchAcheteur;
    });
  }

  get paginatedReceptions(): ReceptionCacao[] {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filteredReceptions.slice(start, start + this.itemsPerPage());
  }

  get totalPages(): number {
    return Math.ceil(this.filteredReceptions.length / this.itemsPerPage());
  }

  get totalQuantiteNet(): number {
    return this.filteredReceptions.reduce((sum, r) => sum + (r.quantiteNet || r.quantiteKg || 0), 0);
  }

  get totalValeur(): number {
    return this.filteredReceptions.reduce((sum, r) => sum + (r.valeurLivree || 0), 0);
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.receptionService.getAll().subscribe({
      next: (data) => {
        this.receptions.set(data);
        this.loadAcheteurs();
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des receptions');
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

  loadReceptionsByAcheteur(acheteurId: string): void {
    if (!acheteurId) {
      this.loadData();
      return;
    }

    this.loading.set(true);
    this.receptionService.getByAcheteur(acheteurId).subscribe({
      next: (data) => {
        this.receptions.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des receptions pour cet acheteur');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  deleteReception(id: string | undefined): void {
    if (!id) return;
    if (confirm('Voulez-vous vraiment supprimer cette reception ?')) {
      this.receptionService.delete(id).subscribe({
        next: () => {
          const acheteurId = this.selectedAcheteurId();
          if (acheteurId) {
            this.loadReceptionsByAcheteur(acheteurId);
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
      this.loadReceptionsByAcheteur(acheteurId);
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