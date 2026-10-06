// src/app/features/cacao/models/remboursement.model.ts

export interface Remboursement {
  id?: string;
  acheteurId: string;
  acheteurNomComplet?: string;
  receptionId: string;
  receptionNumBon?: string;
  quantiteRecue: number;
  quantiteAttendue: number;
  quantiteSurplus: number;
  prixUnitaire: number;
  montantRembourse: number;
  motifRemboursement?: string;
  modePaiement?: string;
  referencePaiement?: string;
  dateRemboursement?: string;
  statut?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}