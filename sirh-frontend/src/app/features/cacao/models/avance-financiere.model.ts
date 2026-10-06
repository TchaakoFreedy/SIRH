// src/app/features/cacao/models/avance-financiere.model.ts

export interface AvanceFinanciere {
  id?: string;
  acheteurId: string;
  montant: number;
  quantiteKg: number;
  prixUnitaire: number;
  dateAvance?: string;
  motif?: string;
  modePaiement?: string;
  referencePaiement?: string;
  acheteurNomComplet?: string;
}