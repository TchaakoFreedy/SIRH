// src/app/features/cacao/cacao.module.ts

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { PermissionGuard } from '../../core/guards/permission.guard';

import { AcheteurListComponent } from './components/acheteur-list/acheteur-list.component';
import { AcheteurDetailComponent } from './components/acheteur-detail/acheteur-detail.component';
import { AcheteurFormComponent } from './components/acheteur-form/acheteur-form.component';
import { AvanceListComponent } from './components/avance-list/avance-list.component';
import { ReceptionListComponent } from './components/reception-list/reception-list.component';
import { RemboursementListComponent } from './components/remboursement-list/remboursement-list.component';

const routes: Routes = [
  {
    path: 'acheteurs',
    component: AcheteurListComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'acheteurs/new',
    component: AcheteurFormComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'acheteurs/:id/edit',
    component: AcheteurFormComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'acheteurs/:id/detail',
    component: AcheteurDetailComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'avances',
    component: AvanceListComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'receptions',
    component: ReceptionListComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: 'remboursements',
    component: RemboursementListComponent,
    canActivate: [PermissionGuard],
    data: { permission: 'CACAO_VIEW' }
  },
  {
    path: '',
    redirectTo: 'acheteurs',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes),
    // Composants standalone importés au lieu d'être déclarés
    AcheteurListComponent,
    AcheteurDetailComponent,
    AcheteurFormComponent,
    AvanceListComponent,
    ReceptionListComponent,
    RemboursementListComponent
  ],
  exports: [
    RouterModule
  ]
})
export class CacaoModule { }