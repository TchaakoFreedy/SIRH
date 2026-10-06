// src/app/features/cacao/components/acheteur-detail/acheteur-detail.component.ts

import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AcheteurService } from '../../services/acheteur.service';
import { AvanceService } from '../../services/avance.service';
import { ReceptionService } from '../../services/reception.service';
import { RemboursementService } from '../../services/remboursement.service';
import { Acheteur } from '../../models/acheteur.model';
import { AvanceFinanciere } from '../../models/avance-financiere.model';
import { ReceptionCacao } from '../../models/reception-cacao.model';
import { Remboursement } from '../../models/remboursement.model';
import { ExportService } from '../../services/export.service';

@Component({
  selector: 'app-acheteur-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acheteur-detail.component.html',
  styleUrls: ['./acheteur-detail.component.scss']
})
export class AcheteurDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private acheteurService = inject(AcheteurService);
  private avanceService = inject(AvanceService);
  private receptionService = inject(ReceptionService);
  private remboursementService = inject(RemboursementService);
  private exportService = inject(ExportService);

  acheteur = signal<Acheteur | null>(null);
  avances = signal<AvanceFinanciere[]>([]);
  receptions = signal<ReceptionCacao[]>([]);
  remboursements = signal<Remboursement[]>([]);
  loading = signal(true);
  error = signal('');

  newAvance: AvanceFinanciere = {
    acheteurId: '',
    montant: 0,
    quantiteKg: 0,
    prixUnitaire: 0
  };

  newReception: ReceptionCacao = {
    acheteurId: '',
    quantiteKg: 0,
    quantiteRefractee: 0,
    prixUnitaire: 0
  };

  newRemboursement: Remboursement = {
    acheteurId: '',
    receptionId: '',
    quantiteAttendue: 0,
    quantiteRecue: 0,
    quantiteSurplus: 0,
    prixUnitaire: 0,
    montantRembourse: 0
  };

  showAvanceForm = signal(false);
  showReceptionForm = signal(false);
  showRemboursementForm = signal(false);
  receptionsNonRemboursees = signal<ReceptionCacao[]>([]);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.newAvance.acheteurId = id;
      this.newReception.acheteurId = id;
      this.newRemboursement.acheteurId = id;
      this.loadDetail(id);
      this.loadReceptionsNonRemboursees(id);
    }
  }

  loadDetail(id: string): void {
    this.loading.set(true);
    this.acheteurService.getById(id).subscribe({
      next: (acheteur) => {
        this.acheteur.set(acheteur);
        this.loadAvances(id);
        this.loadReceptions(id);
        this.loadRemboursements(id);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur chargement acheteur');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  loadAvances(acheteurId: string): void {
    this.avanceService.getByAcheteur(acheteurId).subscribe({
      next: (data) => this.avances.set(data),
      error: (err) => {
        this.error.set('Erreur chargement avances');
        console.error(err);
      }
    });
  }

  loadReceptions(acheteurId: string): void {
    this.receptionService.getByAcheteur(acheteurId).subscribe({
      next: (data) => this.receptions.set(data),
      error: (err) => {
        this.error.set('Erreur chargement receptions');
        console.error(err);
      }
    });
  }

  loadRemboursements(acheteurId: string): void {
    this.remboursementService.getByAcheteur(acheteurId).subscribe({
      next: (data) => this.remboursements.set(data),
      error: (err) => {
        console.error('Erreur chargement remboursements', err);
      }
    });
  }

  loadReceptionsNonRemboursees(acheteurId: string): void {
    this.receptionService.getNonRembourseesByAcheteur(acheteurId).subscribe({
      next: (data) => this.receptionsNonRemboursees.set(data),
      error: (err) => {
        console.error('Erreur chargement receptions non remboursees', err);
      }
    });
  }

  addAvance(): void {
    if (this.newAvance.quantiteKg <= 0) {
      alert('La quantite en kg doit etre positive');
      return;
    }
    if (this.newAvance.prixUnitaire <= 0) {
      alert('Le prix unitaire doit etre positif');
      return;
    }
    this.avanceService.create(this.newAvance).subscribe({
      next: () => {
        this.newAvance.quantiteKg = 0;
        this.newAvance.prixUnitaire = 0;
        this.newAvance.montant = 0;
        this.newAvance.motif = '';
        this.newAvance.modePaiement = '';
        this.loadAvances(this.newAvance.acheteurId);
        this.refreshAcheteur();
        this.showAvanceForm.set(false);
      },
      error: (err) => {
        alert('Erreur enregistrement avance');
        console.error(err);
      }
    });
  }

  addReception(): void {
    if (this.newReception.quantiteKg <= 0) {
      alert('La quantite en kg doit etre positive');
      return;
    }
    if (this.newReception.prixUnitaire <= 0) {
      alert('Le prix unitaire doit etre positif');
      return;
    }
    if (this.newReception.quantiteRefractee && this.newReception.quantiteRefractee > 0) {
      if (this.newReception.quantiteRefractee > this.newReception.quantiteKg) {
        alert('La quantite refractee ne peut pas depasser la quantite apportee');
        return;
      }
      if (!this.newReception.motifRefraction) {
        alert('Le motif de la refraction est requis');
        return;
      }
    }
    this.receptionService.create(this.newReception).subscribe({
      next: () => {
        this.newReception.quantiteKg = 0;
        this.newReception.quantiteRefractee = 0;
        this.newReception.prixUnitaire = 0;
        this.newReception.motifRefraction = '';
        this.newReception.observations = '';
        this.newReception.qualite = '';
        this.loadReceptions(this.newReception.acheteurId);
        this.loadReceptionsNonRemboursees(this.newReception.acheteurId);
        this.refreshAcheteur();
        this.showReceptionForm.set(false);
      },
      error: (err) => {
        alert('Erreur enregistrement reception');
        console.error(err);
      }
    });
  }

  addRemboursement(): void {
    if (!this.newRemboursement.receptionId) {
      alert('Veuillez selectionner une reception');
      return;
    }
    if (this.newRemboursement.quantiteAttendue <= 0) {
      alert('La quantite attendue doit etre positive');
      return;
    }
    if (this.newRemboursement.prixUnitaire <= 0) {
      alert('Le prix unitaire doit etre positif');
      return;
    }
    this.remboursementService.create(this.newRemboursement).subscribe({
      next: () => {
        this.newRemboursement.receptionId = '';
        this.newRemboursement.quantiteAttendue = 0;
        this.newRemboursement.quantiteSurplus = 0;
        this.newRemboursement.prixUnitaire = 0;
        this.newRemboursement.montantRembourse = 0;
        this.newRemboursement.motifRemboursement = '';
        this.newRemboursement.modePaiement = '';
        this.loadRemboursements(this.newRemboursement.acheteurId);
        this.loadReceptions(this.newRemboursement.acheteurId);
        this.loadReceptionsNonRemboursees(this.newRemboursement.acheteurId);
        this.refreshAcheteur();
        this.showRemboursementForm.set(false);
      },
      error: (err) => {
        alert('Erreur enregistrement remboursement');
        console.error(err);
      }
    });
  }

  deleteAvance(id: string | undefined): void {
    if (!id) return;
    if (confirm('Supprimer cette avance ?')) {
      this.avanceService.delete(id).subscribe({
        next: () => {
          this.loadAvances(this.newAvance.acheteurId);
          this.refreshAcheteur();
        },
        error: (err) => {
          alert('Erreur suppression');
          console.error(err);
        }
      });
    }
  }

  deleteReception(id: string | undefined): void {
    if (!id) return;
    const reception = this.receptions().find(r => r.id === id);
    if (reception && reception.remboursementId) {
      alert('Impossible de supprimer une reception ayant un remboursement associe');
      return;
    }
    if (confirm('Supprimer cette reception ?')) {
      this.receptionService.delete(id).subscribe({
        next: () => {
          this.loadReceptions(this.newReception.acheteurId);
          this.loadReceptionsNonRemboursees(this.newReception.acheteurId);
          this.refreshAcheteur();
        },
        error: (err) => {
          alert('Erreur suppression');
          console.error(err);
        }
      });
    }
  }

  deleteRemboursement(id: string | undefined): void {
    if (!id) return;
    if (confirm('Supprimer ce remboursement ?')) {
      this.remboursementService.delete(id).subscribe({
        next: () => {
          this.loadRemboursements(this.newRemboursement.acheteurId);
          this.loadReceptions(this.newRemboursement.acheteurId);
          this.loadReceptionsNonRemboursees(this.newRemboursement.acheteurId);
          this.refreshAcheteur();
        },
        error: (err) => {
          alert('Erreur suppression');
          console.error(err);
        }
      });
    }
  }

  refreshAcheteur(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.acheteurService.getById(id).subscribe({
        next: (acheteur) => this.acheteur.set(acheteur),
        error: (err) => console.error(err)
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/app/cacao/acheteurs']);
  }

  toggleAvanceForm(): void {
    this.showAvanceForm.set(!this.showAvanceForm());
    if (this.showAvanceForm()) {
      this.showReceptionForm.set(false);
      this.showRemboursementForm.set(false);
    }
  }

  toggleReceptionForm(): void {
    this.showReceptionForm.set(!this.showReceptionForm());
    if (this.showReceptionForm()) {
      this.showAvanceForm.set(false);
      this.showRemboursementForm.set(false);
    }
  }

  toggleRemboursementForm(): void {
    this.showRemboursementForm.set(!this.showRemboursementForm());
    if (this.showRemboursementForm()) {
      this.showAvanceForm.set(false);
      this.showReceptionForm.set(false);
      this.loadReceptionsNonRemboursees(this.newRemboursement.acheteurId);
    }
  }

  onReceptionSelect(receptionId: string): void {
    const reception = this.receptionsNonRemboursees().find(r => r.id === receptionId);
    if (reception) {
      this.newRemboursement.quantiteRecue = reception.quantiteNet || reception.quantiteKg || 0;
      this.newRemboursement.prixUnitaire = reception.prixUnitaire || 0;
      this.newRemboursement.receptionNumBon = reception.numBonReception;
      this.calculateRemboursement();
    }
  }

  calculateRemboursement(): void {
    const surplus = this.newRemboursement.quantiteRecue - this.newRemboursement.quantiteAttendue;
    if (surplus > 0) {
      this.newRemboursement.quantiteSurplus = surplus;
      this.newRemboursement.montantRembourse = surplus * this.newRemboursement.prixUnitaire;
    } else {
      this.newRemboursement.quantiteSurplus = 0;
      this.newRemboursement.montantRembourse = 0;
    }
  }

  getSoldeClass(solde: number | undefined): string {
    if (solde === undefined) return '';
    if (solde > 0) return 'text-danger';
    if (solde < 0) return 'text-success';
    return 'text-warning';
  }

  getTotalRemboursements(): number {
    return this.remboursements().reduce((sum, r) => sum + (r.montantRembourse || 0), 0);
  }

  // ========== METHODES D'EXPORT CORRIGEES ==========

  private formatDate(dateValue: string | Date | undefined): string {
    if (!dateValue) return '-';
    const date = typeof dateValue === 'string' ? new Date(dateValue) : dateValue;
    if (isNaN(date.getTime())) return '-';
    return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR');
  }

  private getAcheteurName(): string {
    return this.acheteur()?.nomComplet || 'Acheteur';
  }

  private getZoneCollecte(): string {
    return this.acheteur()?.zoneCollecte || '-';
  }

  exportAvances(): void {
    const acheteur = this.acheteur();
    if (!acheteur) return;
    
    if (this.avances().length === 0) {
      alert('Aucune avance à exporter');
      return;
    }

    const data = this.avances().map(a => ({
      dateAvance: this.formatDate(a.dateAvance),
      quantiteKg: a.quantiteKg || 0,
      prixUnitaire: a.prixUnitaire || 0,
      montant: a.montant || 0,
      motif: a.motif || '-',
      modePaiement: a.modePaiement || '-'
    }));

    const columns = [
      { header: 'Date', dataKey: 'dateAvance' },
      { header: 'Quantité (KG)', dataKey: 'quantiteKg' },
      { header: 'Prix Unitaire (FCFA)', dataKey: 'prixUnitaire' },
      { header: 'Montant (FCFA)', dataKey: 'montant' },
      { header: 'Motif', dataKey: 'motif' },
      { header: 'Mode Paiement', dataKey: 'modePaiement' }
    ];

    const total = data.reduce((sum, a) => sum + (a.montant || 0), 0);
    const name = this.getAcheteurName();
    const zone = this.getZoneCollecte();
    
    this.exportService.exportToPDF(
      data,
      `Avances - ${name}`,
      columns,
      `Zone de collecte: ${zone}`,
      { label: 'Total des avances', value: total }
    );
    
    this.exportService.exportToExcel(
      data,
      `Avances_${name.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0,10)}`,
      'Avances'
    );
  }

  exportReceptions(): void {
    const acheteur = this.acheteur();
    if (!acheteur) return;
    
    if (this.receptions().length === 0) {
      alert('Aucune reception à exporter');
      return;
    }

    const data = this.receptions().map(r => ({
      dateReception: this.formatDate(r.dateReception),
      quantiteKg: r.quantiteKg || 0,
      quantiteRefractee: r.quantiteRefractee || 0,
      quantiteNet: r.quantiteNet || 0,
      prixUnitaire: r.prixUnitaire || 0,
      valeurLivree: r.valeurLivree || 0,
      qualite: r.qualite || '-',
      numBonReception: r.numBonReception || '-',
      motifRefraction: r.motifRefraction || '-'
    }));

    const columns = [
      { header: 'Date', dataKey: 'dateReception' },
      { header: 'Quantité (KG)', dataKey: 'quantiteKg' },
      { header: 'Refraction (KG)', dataKey: 'quantiteRefractee' },
      { header: 'Net (KG)', dataKey: 'quantiteNet' },
      { header: 'Prix Unitaire (FCFA)', dataKey: 'prixUnitaire' },
      { header: 'Valeur Livrée (FCFA)', dataKey: 'valeurLivree' },
      { header: 'Qualité', dataKey: 'qualite' },
      { header: 'N° Bon', dataKey: 'numBonReception' }
    ];

    const total = data.reduce((sum, r) => sum + (r.valeurLivree || 0), 0);
    const name = this.getAcheteurName();
    const zone = this.getZoneCollecte();
    
    this.exportService.exportToPDF(
      data,
      `Receptions - ${name}`,
      columns,
      `Zone de collecte: ${zone}`,
      { label: 'Total des receptions', value: total }
    );
    
    this.exportService.exportToExcel(
      data,
      `Receptions_${name.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0,10)}`,
      'Receptions'
    );
  }

  exportRemboursements(): void {
    const acheteur = this.acheteur();
    if (!acheteur) return;
    
    if (this.remboursements().length === 0) {
      alert('Aucun remboursement à exporter');
      return;
    }

    const data = this.remboursements().map(r => ({
      dateRemboursement: this.formatDate(r.dateRemboursement),
      quantiteRecue: r.quantiteRecue || 0,
      quantiteAttendue: r.quantiteAttendue || 0,
      quantiteSurplus: r.quantiteSurplus || 0,
      prixUnitaire: r.prixUnitaire || 0,
      montantRembourse: r.montantRembourse || 0,
      motifRemboursement: r.motifRemboursement || '-',
      receptionNumBon: r.receptionNumBon || '-',
      modePaiement: r.modePaiement || '-'
    }));

    const columns = [
      { header: 'Date', dataKey: 'dateRemboursement' },
      { header: 'Quantité Recue (KG)', dataKey: 'quantiteRecue' },
      { header: 'Quantité Attendue (KG)', dataKey: 'quantiteAttendue' },
      { header: 'Surplus (KG)', dataKey: 'quantiteSurplus' },
      { header: 'Prix Unitaire (FCFA)', dataKey: 'prixUnitaire' },
      { header: 'Montant Remboursé (FCFA)', dataKey: 'montantRembourse' },
      { header: 'Motif', dataKey: 'motifRemboursement' },
      { header: 'Mode Paiement', dataKey: 'modePaiement' },
      { header: 'N° Bon', dataKey: 'receptionNumBon' }
    ];

    const total = data.reduce((sum, r) => sum + (r.montantRembourse || 0), 0);
    const name = this.getAcheteurName();
    const zone = this.getZoneCollecte();
    
    this.exportService.exportToPDF(
      data,
      `Remboursements - ${name}`,
      columns,
      `Zone de collecte: ${zone}`,
      { label: 'Total des remboursements', value: total }
    );
    
    this.exportService.exportToExcel(
      data,
      `Remboursements_${name.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0,10)}`,
      'Remboursements'
    );
  }

  exportAll(): void {
    const acheteur = this.acheteur();
    if (!acheteur) return;
    
    if (this.avances().length === 0 && this.receptions().length === 0 && this.remboursements().length === 0) {
      alert('Aucune donnée à exporter');
      return;
    }
    
    this.exportService.exportAcheteurData(
      { 
        nomComplet: acheteur.nomComplet || 'Acheteur', 
        zoneCollecte: acheteur.zoneCollecte || '-' 
      },
      this.avances(),
      this.receptions(),
      this.remboursements()
    );
  }
}