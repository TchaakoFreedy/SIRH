// src/app/features/cacao/models/solde-acheteur.model.ts

export interface SoldeAcheteur {
  acheteurId: string;
  acheteurNomComplet: string;
  totalAvances: number;
  totalValeurLivree: number;
  solde: number;
}