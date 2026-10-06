// src/app/features/cacao/models/acheteur.model.ts

export interface Acheteur {
  id?: string;
  employeeId: string;
  zoneCollecte?: string;
  statut?: string;
  nomComplet?: string;
  telephone?: string;
  email?: string;
  totalAvances?: number;
  totalValeurLivree?: number;
  solde?: number;
}