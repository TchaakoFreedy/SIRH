// src/app/features/cacao/models/reception-cacao.model.ts

export interface ReceptionCacao {
  id?: string;
  acheteurId: string;
  dateReception?: string;
  quantiteKg: number;
  quantiteRefractee?: number;
  quantiteNet?: number;
  motifRefraction?: string;
  prixUnitaire: number;
  valeurLivree?: number;
  qualite?: string;
  observations?: string;
  numBonReception?: string;
  acheteurNomComplet?: string;
  montantRembourse?: number;
  remboursementId?: string;
}