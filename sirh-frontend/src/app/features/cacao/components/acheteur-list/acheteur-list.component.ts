// src/app/features/cacao/components/acheteur-list/acheteur-list.component.ts

import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AcheteurService } from '../../services/acheteur.service';
import { Acheteur } from '../../models/acheteur.model';

@Component({
  selector: 'app-acheteur-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './acheteur-list.component.html',
  styleUrls: ['./acheteur-list.component.scss']
})
export class AcheteurListComponent implements OnInit {
  private acheteurService = inject(AcheteurService);

  acheteurs = signal<Acheteur[]>([]);
  loading = signal(false);
  error = signal('');

  searchTerm = signal('');
  selectedZone = signal('');

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  // Utilisation de computed pour les zones
  zones = computed(() => {
    const zonesSet = new Set<string>();
    this.acheteurs().forEach(a => {
      if (a.zoneCollecte) zonesSet.add(a.zoneCollecte);
    });
    return Array.from(zonesSet);
  });

  // Utilisation de computed pour le filtrage
  filteredAcheteurs = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const zone = this.selectedZone();

    return this.acheteurs().filter(a => {
      const matchTerm = !term ||
        a.nomComplet?.toLowerCase().includes(term) ||
        a.telephone?.includes(term) ||
        a.zoneCollecte?.toLowerCase().includes(term);

      const matchZone = !zone || a.zoneCollecte === zone;

      return matchTerm && matchZone;
    });
  });

  // Utilisation de computed pour la pagination
  paginatedAcheteurs = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filteredAcheteurs().slice(start, start + this.itemsPerPage());
  });

  totalPages = computed(() => {
    return Math.ceil(this.filteredAcheteurs().length / this.itemsPerPage());
  });

  ngOnInit(): void {
    this.loadAcheteurs();
  }

  loadAcheteurs(): void {
    this.loading.set(true);
    this.error.set('');
    this.acheteurService.getAll().subscribe({
      next: (data) => {
        this.acheteurs.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des acheteurs');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  deleteAcheteur(id: string | undefined): void {
    if (!id) return;
    if (confirm('Voulez-vous vraiment désactiver cet acheteur ?')) {
      this.acheteurService.delete(id).subscribe({
        next: () => this.loadAcheteurs(),
        error: (err) => {
          this.error.set('Erreur lors de la suppression');
          console.error(err);
        }
      });
    }
  }

  getSoldeClass(solde: number | undefined): string {
    if (solde === undefined) return '';
    if (solde > 0) return 'text-danger';
    if (solde < 0) return 'text-success';
    return 'text-warning';
  }

  getSoldeLabel(solde: number | undefined): string {
    if (solde === undefined) return '-';
    if (solde > 0) return 'Avance excédentaire';
    if (solde < 0) return 'Dette acheteur';
    return 'Équilibré';
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  clearFilter(): void {
    this.selectedZone.set('');
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.selectedZone.set('');
    this.currentPage.set(1);
  }

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

  onPageSizeChange(): void {
    this.currentPage.set(1);
  }

  getVisiblePages(): number[] {
    const total = this.totalPages();
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

  get Math() {
    return Math;
  }
}