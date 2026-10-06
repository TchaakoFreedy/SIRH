package com.fric.sirh.service;

import com.fric.sirh.model.Poste;

import java.util.List;
import java.util.Optional;

public interface PosteService {

    Poste create(Poste poste);

    List<Poste> getAll();

    List<Poste> getActivePostes();

    Optional<Poste> getById(String id);

    Poste update(String id, Poste poste);

    void delete(String id);

    Poste toggleActive(String id);

    List<Poste> getByDepartement(String departementId);

    // ==========================================
    // ✅ NOUVELLES MÉTHODES POUR DIRECTION
    // ==========================================

    /**
     * Récupère tous les postes de l'entreprise de l'utilisateur
     */
    List<Poste> getPostesByUserCompany(String userId);

    /**
     * Récupère les postes actifs de l'entreprise de l'utilisateur
     */
    List<Poste> getActivePostesByUserCompany(String userId);

    /**
     * Vérifie si un poste appartient à l'entreprise de l'utilisateur
     */
    boolean isPosteInUserCompany(String userId, String posteId);

    /**
     * Récupère l'entreprise ID d'un utilisateur
     */
    String getCompanyIdByUserId(String userId);
}