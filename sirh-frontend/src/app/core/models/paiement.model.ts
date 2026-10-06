// src/app/core/models/paiement.model.ts

export type TypePaiement = 'PAIEMENT_SALAIRE' | 'AVANCE' | 'RETENUE';

export interface Paiement {
  id?: string;
  employeeId: string;
  employeeNom?: string;
  employeePrenom?: string;
  employeeMatricule?: string;
  employeePoste?: string;
  employeeDepartement?: string;
  employeeTelephone?: string;
  employeeEmail?: string;
  employeeDateEmbauche?: string;

  type: TypePaiement;
  montant: number;
  motif?: string;

  mois: number;
  annee: number;

  datePaiement?: string;
  numeroRecu?: string;

  salaireMensuel?: number;
  totalAvancesMois?: number;
  totalRetenuesMois?: number;
  totalPayeMois?: number;
  montantNetAPayer?: number;
  montantRestantApres?: number;

  createdBy?: string;
  createdAt?: string;
}

export interface PaiementRequest {
  employeeId: string;
  type: TypePaiement;
  montant: number;
  motif?: string;
  mois: number;
  annee: number;
}

export interface SoldeEmploye {
  employeeId: string;
  employeeNom: string;
  employeePrenom: string;
  employeeMatricule: string;
  employeePoste: string;
  employeeDepartement: string;
  employeeTelephone: string;

  salaireMensuel: number;
  mois: number;
  annee: number;

  totalAvances: number;
  totalRetenues: number;
  totalPaye: number;

  montantNetAPayer: number;
  montantRestant: number;

  salaireConfigure: boolean;
  soldeDisponible: boolean;
}