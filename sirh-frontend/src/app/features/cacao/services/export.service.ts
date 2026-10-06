// src/app/features/cacao/services/export.service.ts

import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Injectable({
  providedIn: 'root'
})
export class ExportService {

  constructor() { }

  // Export Excel
  exportToExcel(data: any[], fileName: string, sheetName: string = 'Données'): void {
    if (!data || data.length === 0) {
      console.warn('Aucune donnée à exporter');
      return;
    }

    // Transformer les clés pour avoir des en-têtes avec majuscules et espaces
    const formattedData = data.map(item => {
      const newItem: any = {};
      Object.keys(item).forEach(key => {
        // Transformer les clés en en-têtes lisibles
        let header = key
          .replace(/([A-Z])/g, ' $1')
          .replace(/^./, str => str.toUpperCase())
          .replace(/Kg/g, '(KG)')
          .replace(/Qte/g, 'Quantité');
        
        // Cas spéciaux
        if (key === 'quantiteKg') header = 'Quantité (KG)';
        else if (key === 'quantiteRefractee') header = 'Refraction (KG)';
        else if (key === 'quantiteNet') header = 'Net (KG)';
        else if (key === 'quantiteRecue') header = 'Quantité Recue (KG)';
        else if (key === 'quantiteAttendue') header = 'Quantité Attendue (KG)';
        else if (key === 'quantiteSurplus') header = 'Surplus (KG)';
        else if (key === 'prixUnitaire') header = 'Prix Unitaire (FCFA)';
        else if (key === 'montant') header = 'Montant (FCFA)';
        else if (key === 'valeurLivree') header = 'Valeur Livrée (FCFA)';
        else if (key === 'montantRembourse') header = 'Montant Remboursé (FCFA)';
        else if (key === 'modePaiement') header = 'Mode Paiement';
        else if (key === 'numBonReception') header = 'N° Bon';
        else if (key === 'motifRefraction') header = 'Motif Réfraction';
        
        newItem[header] = item[key];
      });
      return newItem;
    });

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(formattedData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, sheetName);
    
    // Ajuster la largeur des colonnes
    const colWidths = Object.keys(formattedData[0] || {}).map(key => ({ wch: Math.max(key.length * 2, 15) }));
    ws['!cols'] = colWidths;
    
    XLSX.writeFile(wb, `${fileName}.xlsx`);
  }

  // Export PDF avec mise en page professionnelle
  exportToPDF(
    data: any[], 
    title: string, 
    columns: { header: string, dataKey: string }[],
    subtitle?: string,
    totalRow?: { label: string, value: number }
  ): void {
    if (!data || data.length === 0) {
      console.warn('Aucune donnée à exporter en PDF');
      return;
    }

    const doc = new jsPDF('landscape', 'mm', 'a4');
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();

    // En-tête
    doc.setFillColor(1, 147, 147);
    doc.rect(0, 0, pageWidth, 35, 'F');
    
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(22);
    doc.setFont('helvetica', 'bold');
    doc.text(title, pageWidth / 2, 18, { align: 'center' });
    
    if (subtitle) {
      doc.setFontSize(12);
      doc.setFont('helvetica', 'normal');
      doc.text(subtitle, pageWidth / 2, 28, { align: 'center' });
    }

    // Date d'export
    doc.setTextColor(1, 147, 147);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.text(`Exporté le: ${new Date().toLocaleDateString('fr-FR')} à ${new Date().toLocaleTimeString('fr-FR')}`, pageWidth - 20, 42, { align: 'right' });

    // Tableau avec alignement à droite pour les nombres
    const tableData = data.map(item => {
      const row: any = {};
      columns.forEach(col => {
        let value = item[col.dataKey];
        if (typeof value === 'number') {
          // Ajouter FCFA pour les montants
          const isAmount = col.dataKey.includes('montant') || 
                          col.dataKey.includes('valeur') || 
                          col.dataKey.includes('prix');
          value = value.toFixed(2);
          if (isAmount) {
            value = value + ' FCFA';
          }
        } else if (value instanceof Date) {
          value = value.toLocaleDateString('fr-FR');
        } else if (!value && value !== 0) {
          value = '-';
        }
        row[col.header] = value;
      });
      return row;
    });

    // Définir l'alignement des colonnes
    const columnStyles: any = {};
    columns.forEach((col, index) => {
      const isNumeric = col.dataKey.includes('quantite') || 
                       col.dataKey.includes('montant') || 
                       col.dataKey.includes('valeur') || 
                       col.dataKey.includes('prix') ||
                       col.dataKey.includes('surplus');
      if (isNumeric) {
        columnStyles[index] = { halign: 'right' };
      }
    });

    autoTable(doc, {
      head: [columns.map(col => col.header)],
      body: tableData.map(item => columns.map(col => item[col.header])),
      startY: 48,
      theme: 'grid',
      styles: {
        fontSize: 8,
        cellPadding: 2,
        valign: 'middle'
      },
      headStyles: {
        fillColor: [1, 147, 147],
        textColor: [255, 255, 255],
        fontSize: 9,
        fontStyle: 'bold',
        halign: 'center'
      },
      alternateRowStyles: {
        fillColor: [240, 248, 248]
      },
      columnStyles: columnStyles,
      margin: { left: 10, right: 10 }
    });

    // Total si fourni
    if (totalRow) {
      const finalY = (doc as any).lastAutoTable.finalY + 8;
      doc.setFillColor(1, 147, 147);
      doc.setDrawColor(1, 147, 147);
      doc.rect(10, finalY - 2, pageWidth - 20, 10, 'F');
      
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      doc.text(
        `${totalRow.label}: ${totalRow.value.toFixed(2)} FCFA`,
        pageWidth - 20,
        finalY + 5,
        { align: 'right' }
      );
    }

    // Pied de page
    const pageCount = doc.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      doc.setTextColor(1, 147, 147);
      doc.setFontSize(8);
      doc.setFont('helvetica', 'normal');
      doc.text(
        `Page ${i} sur ${pageCount}`,
        pageWidth / 2,
        pageHeight - 5,
        { align: 'center' }
      );
    }

    doc.save(`${title.replace(/\s+/g, '_')}.pdf`);
  }

  // Export combiné pour un acheteur
  exportAcheteurData(
    acheteur: { nomComplet: string, zoneCollecte: string },
    avances: any[],
    receptions: any[],
    remboursements: any[]
  ): void {
    const prefix = acheteur.nomComplet.replace(/\s+/g, '_');
    const date = new Date().toISOString().slice(0, 10);
    const baseName = `${prefix}_${date}`;
    const subtitle = `Zone de collecte: ${acheteur.zoneCollecte}`;

    // Export Avances
    if (avances.length > 0) {
      const avanceColumns = [
        { header: 'Date', dataKey: 'dateAvance' },
        { header: 'Quantité (KG)', dataKey: 'quantiteKg' },
        { header: 'Prix Unitaire (FCFA)', dataKey: 'prixUnitaire' },
        { header: 'Montant (FCFA)', dataKey: 'montant' },
        { header: 'Motif', dataKey: 'motif' },
        { header: 'Mode Paiement', dataKey: 'modePaiement' }
      ];
      const totalAvances = avances.reduce((sum, a) => sum + (a.montant || 0), 0);
      this.exportToPDF(
        avances,
        `Avances - ${acheteur.nomComplet}`,
        avanceColumns,
        subtitle,
        { label: 'Total des avances', value: totalAvances }
      );
      this.exportToExcel(avances, `${baseName}_Avances`, 'Avances');
    }

    // Export Réceptions
    if (receptions.length > 0) {
      const receptionColumns = [
        { header: 'Date', dataKey: 'dateReception' },
        { header: 'Quantité (KG)', dataKey: 'quantiteKg' },
        { header: 'Refraction (KG)', dataKey: 'quantiteRefractee' },
        { header: 'Net (KG)', dataKey: 'quantiteNet' },
        { header: 'Prix Unitaire (FCFA)', dataKey: 'prixUnitaire' },
        { header: 'Valeur Livrée (FCFA)', dataKey: 'valeurLivree' },
        { header: 'Qualité', dataKey: 'qualite' },
        { header: 'N° Bon', dataKey: 'numBonReception' }
      ];
      const totalReceptions = receptions.reduce((sum, r) => sum + (r.valeurLivree || 0), 0);
      this.exportToPDF(
        receptions,
        `Réceptions - ${acheteur.nomComplet}`,
        receptionColumns,
        subtitle,
        { label: 'Total des réceptions', value: totalReceptions }
      );
      this.exportToExcel(receptions, `${baseName}_Receptions`, 'Réceptions');
    }

    // Export Remboursements
    if (remboursements.length > 0) {
      const remboursementColumns = [
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
      const totalRemboursements = remboursements.reduce((sum, r) => sum + (r.montantRembourse || 0), 0);
      this.exportToPDF(
        remboursements,
        `Remboursements - ${acheteur.nomComplet}`,
        remboursementColumns,
        subtitle,
        { label: 'Total des remboursements', value: totalRemboursements }
      );
      this.exportToExcel(remboursements, `${baseName}_Remboursements`, 'Remboursements');
    }
  }
}